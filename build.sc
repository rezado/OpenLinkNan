package build

import mill.*
import scalalib.*

def defaultScalaVersion = "2.13.16"
def depVersion = Map(
  "scalatest" -> mvn"org.scalatest::scalatest:3.2.7",
  "chisel" -> mvn"org.chipsalliance::chisel:6.7.0",
  "chisel-plugin" -> mvn"org.chipsalliance:::chisel-plugin:6.7.0",
  "chiseltest" -> mvn"edu.berkeley.cs::chiseltest:6.0.0",
  "llvm-firtool" -> mvn"org.chipsalliance:llvm-firtool:1.62.1",
  "json4s-jackson" -> mvn"org.json4s::json4s-jackson:4.0.7",
  "mainargs" -> mvn"com.lihaoyi::mainargs:0.5.0",
  "sourcecode" -> mvn"com.lihaoyi::sourcecode:0.4.4",
)

trait CommonModule extends ScalaModule {
  override def scalaVersion = defaultScalaVersion

  override def scalacPluginMvnDeps = Seq(depVersion("chisel-plugin"))

  override def scalacOptions = super.scalacOptions() ++
    Seq("-language:reflectiveCalls", "-Ymacro-annotations", "-Ytasty-reader")

  override def mvnDeps = super.mvnDeps() ++ Seq(
    depVersion("chisel"),
    depVersion("llvm-firtool")
  )
}

object `package` extends SbtModule with CommonModule {
  override def moduleDeps = super.moduleDeps ++ Seq(
    dependencies.cpl2,
    dependencies.zhujiang,
    dependencies.nanhu,
    dependencies.aia
  )
  override def scalacOptions = super.scalacOptions() ++ Seq("-deprecation")
  def mainClass = Some("linknan.generator.SocGenerator")

  object test extends SbtTests {
    override def mvnDeps = super.mvnDeps() ++ Seq(
      depVersion("scalatest")
    )
    def mainClass = Some("lntest.top.SimGenerator")
    def testFramework = "org.scalatest.tools.Framework"
  }

  object dependencies extends CommonModule {

    object cde extends CommonModule {
      object cde extends CommonModule
    }

    object diplomacy extends CommonModule {
      object diplomacy extends CommonModule {
        override def moduleDeps = super.moduleDeps ++ Seq(cde.cde)
        override def mvnDeps = super.mvnDeps() ++ Seq(depVersion("sourcecode"))
      }
    }

    object hardfloat extends CommonModule {
      object hardfloat extends CommonModule
    }

    object `rocket-chip` extends CommonModule {
      override def moduleDeps = Seq(hardfloat.hardfloat, cde.cde, diplomacy.diplomacy, macros)
      override def mvnDeps = super.mvnDeps() ++ Seq(
        depVersion("json4s-jackson"),
        depVersion("mainargs")
      )

      object macros extends CommonModule {}
    }

    object `xs-utils` extends SbtModule with CommonModule {
      override def moduleDeps = super.moduleDeps ++ Seq(`rocket-chip`)
    }

    object aia extends SbtModule with CommonModule {
      override def moduleDeps = super.moduleDeps ++ Seq(`xs-utils`)
    }

    object difftest extends SbtModule with CommonModule

    object zhujiang extends SbtModule with CommonModule {
      override def moduleDeps = super.moduleDeps ++ Seq(`xs-utils`)
    }

    object cpl2 extends SbtModule with CommonModule {
      override def moduleDeps = super.moduleDeps ++ Seq(`xs-utils`)
    }

    object nanhu extends SbtModule with CommonModule {
      override def moduleDeps = super.moduleDeps ++ Seq(`xs-utils`, difftest, YunSuan)
      override def mvnDeps = super.mvnDeps() ++ Seq(
        depVersion("scalatest"),
        depVersion("chiseltest")
      )

      object YunSuan extends SbtModule with CommonModule
    }

  }
}
