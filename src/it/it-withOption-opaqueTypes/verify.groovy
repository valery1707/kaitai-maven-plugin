String log = new File(basedir, "build.log").text
File target = new File(basedir, "target");
File kaitaiCache = target.toPath().parent.parent.parent.resolve("local-repo/.cache/kaitai").toFile()
File targetTemplate = target.toPath().resolve("classes/kaitai/doc_container.ksy").toFile()
File generated = new File(target, "generated-sources");
File generatedParser = generated.toPath().resolve("kaitai/src/name/valery1707/kaitai/it/DocContainer.java").toFile()

assert log.contains("KaiTai distribution: Prepare cache directory")
assert log.contains("KaiTai distribution: Downloading")
assert log.contains("KaiTai distribution: Extracting")
assert log.contains("kaitai-struct-compiler")
assert log.contains("[INFO] BUILD SUCCESS")
assert kaitaiCache.exists() && kaitaiCache.renameTo(kaitaiCache.toPath().resolveSibling(target.parentFile.name).toFile())
assert target.exists()
assert targetTemplate.exists()
assert generated.exists()
assert generatedParser.exists() && generatedParser.isFile()
