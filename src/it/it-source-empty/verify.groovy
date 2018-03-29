String log = new File(basedir, "build.log").text
File target = new File(basedir, "target");
File generated = new File(target, "generated-sources");

assert log.contains("Skip KaiTai generation: Source directory does not contain KaiTai templates")
assert target.exists()
assert !generated.exists()
