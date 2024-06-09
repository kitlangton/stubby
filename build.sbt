inThisBuild(
  List(
    name           := "stubby",
    normalizedName := "stubby",
    organization   := "io.github.kitlangton",
    homepage       := Some(url("https://github.com/kitlangton/stubby")),
    licenses       := List("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0")),
    developers := List(
      Developer("kitlangton", "Kit Langton", "kit.langton@gmail.com", url("https://github.com/kitlangton"))
    ),
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision
  )
)

Global / onChangedBuildSource := ReloadOnSourceChanges

////////////////////////
// sbt-github-actions //
////////////////////////
ThisBuild / githubWorkflowJavaVersions := Seq(JavaSpec.temurin("17"))

ThisBuild / githubWorkflowEnv := Map("JAVA_OPTS" -> "-Xmx4g")
ThisBuild / githubWorkflowTargetTags ++= Seq("v*")
ThisBuild / githubWorkflowPublishTargetBranches :=
  Seq(
    RefPredicate.StartsWith(Ref.Tag("v")),
    RefPredicate.Equals(Ref.Branch("main"))
  )

ThisBuild / githubWorkflowPublish := Seq(
  WorkflowStep.Sbt(
    commands = List("ci-release"),
    name = Some("Publish project"),
    env = Map(
      "PGP_PASSPHRASE"    -> "${{ secrets.PGP_PASSPHRASE }}",
      "PGP_SECRET"        -> "${{ secrets.PGP_SECRET }}",
      "SONATYPE_PASSWORD" -> "${{ secrets.SONATYPE_PASSWORD }}",
      "SONATYPE_USERNAME" -> "${{ secrets.SONATYPE_USERNAME }}"
    )
  )
)

/////////////////////////
// Project Definitions //
/////////////////////////

val scala3Version = "3.4.2"
val zioVersion    = "2.1.2"

lazy val root = project
  .in(file("."))
  .settings(
    name         := "stubby",
    scalaVersion := scala3Version,
    libraryDependencies ++= Seq(
      "dev.zio"        %% "zio"             % zioVersion,
      "dev.zio"        %% "zio-test"        % zioVersion    % Test,
      "dev.zio"        %% "zio-test-sbt"    % zioVersion    % Test,
      "org.scala-lang" %% "scala3-compiler" % scala3Version % "provided"
    )
  )
