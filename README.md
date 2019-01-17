[![Maven Central](https://maven-badges.herokuapp.com/maven-central/name.valery1707.kaitai/kaitai-maven-plugin/badge.svg)](https://maven-badges.herokuapp.com/maven-central/name.valery1707.kaitai/kaitai-maven-plugin)
[![License](https://img.shields.io/github/license/valery1707/kaitai-maven-plugin.svg)](http://opensource.org/licenses/MIT)

[![Build Status](https://travis-ci.org/valery1707/kaitai-maven-plugin.svg?branch=master)](https://travis-ci.org/valery1707/kaitai-maven-plugin)
[![Build status](https://ci.appveyor.com/api/projects/status/bbvjf5q9faru09xp/branch/master?svg=true)](https://ci.appveyor.com/project/valery1707/kaitai-maven-plugin/branch/master)
[![CircleCI](https://circleci.com/gh/valery1707/kaitai-maven-plugin/tree/master.svg?style=svg)](https://circleci.com/gh/valery1707/kaitai-maven-plugin/tree/master)

[![DevOps By Rultor.com](http://www.rultor.com/b/valery1707/kaitai-maven-plugin)](http://www.rultor.com/p/valery1707/kaitai-maven-plugin)

Maven plugin for [Kaitai](http://kaitai.io/): declarative language to generate binary data parsers.

This plugin has the following goals:
* `kaitai:generate`: generate java-sources for kaitai-templates

## Usage flow

1. Add last version of [kaitai-maven-plugin](https://maven-badges.herokuapp.com/maven-central/name.valery1707.kaitai/kaitai-maven-plugin) into plugins section
1. Configure plugin execution with `<executions><execution><id>generate</id><goals><goal>generate</goal></goals></execution></executions>`
1. Add last version of [kaitai-struct-runtime](https://maven-badges.herokuapp.com/maven-central/io.kaitai/kaitai-struct-runtime) into compile dependencies
1. Add some [kaitai templates](http://formats.kaitai.io/) into `src/main/resources/kaitai`
1. *(Optionally)* Modify [plugin configuration](#plugin-parameters)
1. Build project with goal `package` or manually generate sources with goal `kaitai:generate`

## Working examples

See [kaitai-java-demo](https://github.com/valery1707/kaitai-java-demo).

## Plugin parameters

| Name            | Type         | Since | Description                                                                                                             |
|-----------------|--------------|-------|-------------------------------------------------------------------------------------------------------------------------|
| skip            | boolean      | 0.1.0 | Skip plugin execution (don't read/validate any files, don't generate any java types).<br><br>**Default**: `false`       |
| url             | java.net.URL | 0.1.0 | Direct link onto [KaiTai universal zip archive](http://kaitai.io/#download).<br><br>**Default**: Detected from version  |
| version         | String       | 0.1.0 | Version of [KaiTai](http://kaitai.io/#download) library.<br><br>**Default**: `0.8`                                      |
| cacheDir        | java.io.File | 0.1.0 | Cache directory for download KaiTai library.<br><br>**Default**: `build/tmp/kaitai-cache`                               |
| sourceDirectory | java.io.File | 0.1.0 | Source directory with [Kaitai Struct language](http://formats.kaitai.io/) files.<br><br>**Default**: src/main/resources/kaitai |
| includes        | String[]     | 0.1.0 | Include wildcard pattern list.<br><br>**Default**: ["*.ksy"]                                                            |
| excludes        | String[]     | 0.1.0 | Exclude wildcard pattern list.<br><br>**Default**: []                                                                   |
| output          | java.io.File | 0.1.0 | Target directory for generated Java source files.<br><br>**Default**: `build/generated/kaitai`                          |
| packageName     | String       | 0.1.0 | Target package for generated Java source files.<br><br>**Default**: Trying to get project's group or `kaitai` otherwise |
| executionTimeout| Long         | 0.1.3 | Timeout for execution operations.<br><br>**Default**: `5000` |
| fromFileClass   | String       | 0.1.3 | Classname with custom KaitaiStream implementations for static builder `fromFile(...)`|
| opaqueTypes     | Boolean      | 0.1.3 | Allow use opaque (external) types in ksy. See more in [documentation](http://doc.kaitai.io/user_guide.html#opaque-types).|

### Useful commands

* Execute integration test: `mvn clean verify -P run-its`
* Execute checkstyle (report will be stored in `target/site/checkstyle.html`): `mvn checkstyle:checkstyle`
