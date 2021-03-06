val scala3Version = "3.0.2"
val javaVersion = "11"
val projectName = "scala3-sandbox"
val projectVersion = "0.1.0"

def commonSettings(projectName: String) = {
  Seq(
      name := projectName,
      version := projectVersion,
      scalaVersion := scala3Version,
      scalafmtOnCompile := true,
   ) ++ scalacSettings
}

lazy val scalacSettings = Seq(
  scalacOptions ++= Seq("-deprecation", "-feature", s"-Xtarget:${javaVersion}"),
  javacOptions ++= Seq("-source", javaVersion, "-target", javaVersion)
)

lazy val `scala3-sandbox` = project
  .in(file("."))
  .aggregate(syntax, `webapp-akka`)

lazy val `syntax` = project.in(file("syntax"))
  .settings(
    commonSettings("syntax"),
    libraryDependencies ++= Seq(
      ("org.scalameta" %% "scalameta" % "4.4.27").cross(CrossVersion.for3Use2_13),
    )
  )

val akkaVersion = "2.6.16"
val akkaHttpVersion = "10.2.6"
val circeVersion = "0.14.1"

val circeDependencies = Seq(
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
)

val sangriaVersion = "2.1.3"
val sangriaDependencies = Seq(
  ("org.sangria-graphql" %% "sangria" % sangriaVersion).cross(CrossVersion.for3Use2_13),
  "org.sangria-graphql" %% "sangria-circe" % "1.3.2"
)

lazy val `webapp-akka` = project.in(file("webapp_akka"))
  .settings(
    commonSettings("webapp-akka"),
    run / fork := true,
    run / connectInput := true,
    libraryDependencies ++= circeDependencies ++ Seq(
      "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,
      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
    ).map(_.cross(CrossVersion.for3Use2_13)) ++
      sangriaDependencies ++ Seq(
      "ch.qos.logback" % "logback-classic" % "1.2.6" % Runtime,
    )
  )