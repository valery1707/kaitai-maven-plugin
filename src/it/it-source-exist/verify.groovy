String log = new File(basedir, "build.log").text
File target = new File(basedir, "target");
File generated = new File(target, "generated-sources");

assert log.contains("KaiTai distribution: Prepare cache directory")
assert log.contains("KaiTai distribution: Downloading")
assert log.contains("KaiTai distribution: Extracting")
assert log.contains("kaitai-struct-compiler")
assert target.exists()
assert generated.exists()
