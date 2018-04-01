# Prepare

### Copy

```bash
cp -v ~/.gnupg/pubring.gpg ./
cp -v ~/.gnupg/secring.gpg ./
cp -v settings-template.xml settings.xml
```

### Generate

```bash
ssh-keygen -f github-deploy.key -N ''
```
Upload to [project's deploy keys](https://github.com/valery1707/kaitai-maven-plugin/settings/keys) with name `Rultor` and write access.

### Fill

```bash
nano setting.xml
```

### Sign

```bash
rultor-remote.sh -p valery1707/kaitai-maven-plugin pubring.gpg secring.gpg settings.xml github-deploy.key github-deploy.key.pub
```

### Clean

```bash
rm -v pubring.gpg secring.gpg settings.xml github-deploy.key github-deploy.key.pub
```

# Use

In comment to Issue/PR add text:
```
@rultor deploy, tag=`0.1.11`, next=`0.2.0`
```
Where
* `tag` - version for release
* `next` - version for next development iteration
* both of them not required:
  * `tag` - get current version from `pom.xml`
  * `next` - increment `tag` in 3rd part
