ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.2.1"

ThisBuild / organization := "eu.suranyi"

lazy val compiler_plugin = (project in file("compiler_plugin"))
  .settings(
    name := "compiler_plugin",
//    idePackagePrefix := Some("eu.suranyi.scala.compiler.plugin"),
    libraryDependencies += "org.scala-lang" %% "scala3-compiler" % scalaVersion.value % "provided"
  )

lazy val example = (project in file("example"))
  .dependsOn(compiler_plugin)
  .settings(
    (Compile/compile) := ((Compile/compile) dependsOn (compiler_plugin/Compile/packageBin)).value,
    name := "example",
    scalacOptions += "-feature",
//    scalacOptions += "-Xprint:AsyncMacroTransform$",
//    scalacOptions += "-P:AsyncTransform:",
    scalacOptions += s"-Xplugin:${(compiler_plugin/Compile/packageBin/packagedArtifact).value._2.getAbsolutePath}",
    libraryDependencies += "com.github.rssh" %% "dotty-cps-async" % "0.9.14"
  )
