String log = new File(basedir, "build.log").text
File target = new File(basedir, "target");

assert log.contains("Skip KaiTai generation: skip=true")
assert !target.exists()
