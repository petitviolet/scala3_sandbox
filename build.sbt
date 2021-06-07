val scala3Version = "3.0.0"
val projectName = "scala3-sandbox"
val projectVersion = "0.1.0"

lazy val root = project
  .in(file("."))
  .settings(
    name := projectName,
    version := projectVersion,
    scalaVersion := scala3Version,
    libraryDependencies += "com.novocode" % "junit-interface" % "0.11" % "test"
  )
