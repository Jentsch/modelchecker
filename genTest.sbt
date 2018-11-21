import java.io._

import scala.meta._
import scala.meta.contrib.DocToken.CodeBlock
import scala.meta.contrib._

val gen = TaskKey[Seq[File]]("gen", "Generates tests out of scaladoc code snippets")

Test / sourceGenerators += gen

gen := {

  val log = streams.value.log

  val outDir = target.value / "genTest"

  outDir.mkdirs()

  var seenCodes = Set.empty[Int]


  def writeTests(writer: PrintWriter,
                 tree: Tree,
                 comments: AssociatedComments): Unit = {
    comments.leading(tree).foreach { doc =>
      val name = tree match {
        case Defn.Class(_, name, _, _, _) => name.value
        case Defn.Def(_, name, _, _, _, _) => name.value
        case Defn.Trait(_, name, _, _, _) => name.value
        case _ => tree.getClass.getName
      }

      val parsed = ScaladocParser.parseScaladoc(doc)

      val cases = parsed.getOrElse(Seq.empty).collect {
        case DocToken(CodeBlock, _, Some(code)) => code
      }

      val onlyOne = cases.size == 1

      cases.zipWithIndex.foreach {
        case (code, index) =>
          val pos = index + 1 match {
            case 1 if onlyOne => ""
            case 1 => "first "
            case 2 => "second "
            case 3 => "third "
            case n => n.toString ++ "th "
          }
          if (!seenCodes(code.hashCode)) {
            writer.println(s"""
                              |  it should "match ${pos}example in $name" in {
                              |    $code
                              |  }
           """.stripMargin)
          }

          seenCodes += code.hashCode
      }
    }

    tree.children.foreach(writeTests(writer, _, comments))
  }

  val allScalaFiles =
    (sourceDirectory.value ** "*.scala").get()

  val updateFiles =
    allScalaFiles
      .map(source =>
        source -> outDir / (source.base + "Test.scala"))
      .filter {
        case (source, target) =>
          source.newerThan(target)
      }

  updateFiles.map { case (file, outFile) =>
    log.info(s"processing ${file.name}")

    val parsed = file.parse[Source].get

    val docs: AssociatedComments = AssociatedComments(parsed.tokens)

    val topLevel = parsed.children

    val pack = topLevel.collectFirst { case t: Member => t }.get

    if (outFile.exists())
      outFile.delete()

    val out = new PrintWriter(new BufferedWriter(new FileWriter(outFile)))

    try {
      out.println(s"package ${pack.name}")
      out.println()
      out.println("import org.scalatest._")
      out.println()

      out.println(
        s"class ${file.base}Test extends FlatSpec with Matchers {")

      pack.children.foreach(writeTests(out, _, docs))

      out.println("}")
    } finally {
      out.close()
    }

    outFile
  }

}
