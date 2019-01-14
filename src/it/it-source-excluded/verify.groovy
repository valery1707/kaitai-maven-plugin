String log = new File(basedir, "build.log").text
File target = new File(basedir, "target");
File kaitaiCache = target.toPath().parent.parent.parent.resolve("local-repo/.cache/kaitai").toFile()
File targetTemplate = target.toPath().resolve("classes/kaitai/ico.ksy").toFile()
File generated = new File(target, "generated-sources");

assert log.contains("Skip KaiTai generation: Source directory does not contain KaiTai templates")
assert !kaitaiCache.exists()
assert target.exists()
assert targetTemplate.exists()
assert !generated.exists()
