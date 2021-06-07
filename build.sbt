val scala3Version = "3.0.0"
val projectName = "scala3-sandbox"
val projectVersion = "0.1.0"

def commonSettings = {
  Seq(
      version := projectVersion,
      scalaVersion := scala3Version,
      libraryDependencies += "com.novocode" % "junit-interface" % "0.11" % "test",
      scalafmtOnCompile := true,
     )
}

lazy val `scala3-sandbox` = project
  .in(file("."))
  .settings(
    commonSettings,
    name := "scala3-sandbox",
  )
