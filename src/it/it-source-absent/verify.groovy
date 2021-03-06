String log = new File(basedir, "build.log").text
File target = new File(basedir, "target");
File kaitaiCache = target.toPath().parent.parent.parent.resolve("local-repo/.cache/kaitai").toFile()

assert log.contains("Skip KaiTai generation: Source directory does not exists")
assert !kaitaiCache.exists()
assert !target.exists()
