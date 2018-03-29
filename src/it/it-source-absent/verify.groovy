String log = new File(basedir, "build.log").text
File target = new File(basedir, "target");

assert log.contains("Skip KaiTai generation: Source directory does not exists")
assert !target.exists()
