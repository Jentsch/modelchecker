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

  allScalaFiles.foreach { file =>
    println(s"processing ${file.name}")
    val source = file.contentAsString

    val parsed = source.parse[Source].get

    val docs: AssociatedComments = AssociatedComments(parsed.tokens)

    val topLevel = parsed.children

    val pack = topLevel.collectFirst { case t: Member => t }.get

    val outFile: File = outDir / s"${file.nameWithoutExtension}Test.scala"

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
      val parsed = ScaladocParser.parseScaladoc(doc)

      val cases = parsed.getOrElse(Seq.empty).collect {
        case DocToken(CodeBlock, _, Some(code)) => code
      }

      cases.foreach { code =>
        if (! seenCodes(code.hashCode)) {
          writer.println(
            s"""
             |  it should "match doc ${code.hashCode}" in {
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
