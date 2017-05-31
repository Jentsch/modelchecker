import java.io.{FileOutputStream, PrintStream, PrintWriter}

import scala.meta._
import scala.meta.contrib.DocToken.CodeBlock
import scala.meta.contrib._
import better.files._

object GenerateTests extends App {

  val baseDir = File(".")

  val outDir = baseDir / "target" / "genTest" / "scala"

  outDir.createDirectories()

  var seenCodes = Set.empty[Int]

  val allScalaFiles: Files =
    (baseDir / "src" / "main" / "scala").listRecursively
      .filter(_.extension.contains(".scala"))

  val updateFiles =
    allScalaFiles
      .map(source =>
        source -> outDir / (source.nameWithoutExtension + "Test.scala"))
      .filter {
        case (source, target) =>
          !target.exists ||
            (source.lastModifiedTime isAfter target.lastModifiedTime)
      }

  updateFiles.foreach {
    case (file, outFile) =>
      println(s"processing ${file.name}")
      val source = file.contentAsString

      val parsed = source.parse[Source].get

      val docs: AssociatedComments = AssociatedComments(parsed.tokens)

      val topLevel = parsed.children

      val pack = topLevel.collectFirst { case t: Member => t }.get

      outFile.delete(swallowIOExceptions = true)

      outFile.printWriter().foreach { out =>
        out.println(s"package ${pack.name}")
        out.println()
        out.println("import org.scalatest._")
        out.println()

        out.println(
          s"class ${file.nameWithoutExtension}Test extends FlatSpec with Matchers {")

        pack.children.foreach(writeTests(out, _, docs))

        out.println("}")

      }

  }

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

      val onlyOne = cases.size == 2

      cases.zipWithIndex.foreach {
        case (code, index) =>
          val pos = (index + 1) match {
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
}
