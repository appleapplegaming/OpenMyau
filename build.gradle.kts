import org.apache.commons.lang3.SystemUtils
plugins {
    idea
    java
    id("org.polyfrost.loom") version "1.6.polyfrost.6"
    id("dev.architectury.architectury-pack200") version "0.1.3"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}
//Constants:
val baseGroup: String by project
val mcVersion: String by project
val version: String by project
val mixinGroup = "$baseGroup.mixin"
val modid: String by project
val jarName: String by project
val transformerFile = file("src/main/resources/accesstransformer.cfg")
// Toolchains:
java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}
// Minecraft configuration:
loom {
    log4jConfigs.from(file("log4j2.xml"))

    runConfigs {
        named("client") {
            // Set mixin debug as a JVM system property
            vmArg("-Dmixin.debug=true")

            // Add tweakClass argument (legacy Forge+Mixin)
            programArg("--tweakClass")
            programArg("org.spongepowered.asm.launch.MixinTweaker")

            if (SystemUtils.IS_OS_MAC_OSX) {
                vmArgs.remove("-XstartOnFirstThread")
            }
        }

        remove(getByName("server"))
    }

    forge {
        pack200Provider.set(dev.architectury.pack200.java.Pack200Adapter())
        mixinConfig("mixins.$modid.json")
        if (transformerFile.exists()) {
            println("Installing access transformer")
            accessTransformer(transformerFile)
        }
    }

    mixin {
        defaultRefmapName.set("mixins.$modid.refmap.json")
    }
}
sourceSets.main {
    output.setResourcesDir(sourceSets.main.flatMap { it.java.classesDirectory })
}
// Dependencies:
repositories {
    mavenCentral()
    maven("https://repo.spongepowered.org/maven/")
    // If you don't want to log in with your real minecraft account, remove this line
    maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1")
}
val shadowImpl: Configuration by configurations.creating {
    configurations.implementation.get().extendsFrom(this)
}
dependencies {
    minecraft("com.mojang:minecraft:1.8.9")
    mappings("de.oceanlabs.mcp:mcp_stable:22-1.8.9")
    forge("net.minecraftforge:forge:1.8.9-11.15.1.2318-1.8.9")

    shadowImpl("org.spongepowered:mixin:0.7.11-SNAPSHOT") { isTransitive = false }
    annotationProcessor("org.spongepowered:mixin:0.8.5-SNAPSHOT")

    // ✅ add this line
    annotationProcessor("com.google.code.gson:gson:2.10.1")
    annotationProcessor("com.google.guava:guava:33.0.0-jre")

    runtimeOnly("me.djtheredstoner:DevAuth-forge-legacy:1.2.1")
}
// Tasks:
tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    // Java 8 bytecode, even though toolchain is 21
    options.release.set(8)
}
tasks.withType(org.gradle.jvm.tasks.Jar::class) {
    archiveBaseName.set(jarName)
    manifest.attributes.run {
        this["FMLCorePluginContainsFMLMod"] = "true"
        this["ForceLoadAsMod"] = "true"
        // If you don't want mixins, remove these lines
        this["TweakClass"] = "org.spongepowered.asm.launch.MixinTweaker"
        this["MixinConfigs"] = "mixins.$modid.json"
	    if (transformerFile.exists())
			this["FMLAT"] = "${modid}_at.cfg"
    }
}
tasks.processResources {
    inputs.property("version", project.version)
    inputs.property("mcversion", mcVersion)
    inputs.property("modid", modid)
    inputs.property("basePackage", baseGroup)
    filesMatching(listOf("mcmod.info", "mixins.$modid.json","version.json")) {
        expand(inputs.properties)
    }
    rename("accesstransformer.cfg", "META-INF/${modid}_at.cfg")
}
val remapJar by tasks.named<net.fabricmc.loom.task.RemapJarTask>("remapJar") {
    archiveClassifier.set("")
    from(tasks.shadowJar)
    input.set(tasks.shadowJar.get().archiveFile)
}
tasks.jar {
    archiveClassifier.set("without-deps")
    destinationDirectory.set(layout.buildDirectory.dir("intermediates"))
}
tasks.shadowJar {
    destinationDirectory.set(layout.buildDirectory.dir("intermediates"))
    archiveClassifier.set("non-obfuscated-with-deps")
    configurations = listOf(shadowImpl)
    doLast {
        configurations.forEach {
            println("Copying dependencies into mod: ${it.files}")
        }
    }
    // If you want to include other dependencies and shadow them, you can relocate them in here
    fun relocate(name: String) = relocate(name, "$baseGroup.deps.$name")
}
tasks.assemble.get().dependsOn(tasks.remapJar)

tasks.named<JavaExec>("runClient") {
    setExecutable("C:\\Program Files (x86)\\Java\\jre1.8.0_481\\bin\\java.exe");
}
