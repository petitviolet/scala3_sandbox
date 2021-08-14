val scala3Version = "3.0.0"
val projectName = "scala3-sandbox"
val projectVersion = "0.1.0"

def commonSettings(projectName: String) = {
  Seq(
      name := projectName,
      version := projectVersion,
      scalaVersion := scala3Version,
      libraryDependencies += "com.novocode" % "junit-interface" % "0.11" % "test",
      scalafmtOnCompile := true,
   )
}

lazy val `scala3-sandbox` = project
  .in(file("."))
  .aggregate(syntax)

lazy val `syntax` = project.in(file("syntax"))
  .settings(
    commonSettings("syntax")
  )