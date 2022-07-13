# 使用git作为版本号

需要tag, tag的格式为: x.y.z

## git命令

1. 返回当前项目的提交次数，与分支无关

```
git rev-list --all --count HEAD
```

2. 返回当前分支的提交次数

```
git rev-list --count HEAD
```

3. 返回离当前提交最近的tag

```
git describe --tags --dirty="-test"
```
此指令只能在至少有一个tag的前提下使用，会返回       
`<距离当前最近的tag>-<该tag后的提交次数>-g<最新提交的前7位HASH值>-<如果该版本未提交，则会添加后缀-test>`
的固定格式      
例如`0.1.0-3-gxxxxxxx-test`表示tag0.1.0之后的第3次提交，且该版本未提交到git     
如果直接返回`0.1.0`则表示tag0.1.0之后无提交

## 使用git作为版本号

- 将**git提交次数**作为`versionCode`
- 将**tag+提交次数**作为`versionName`
- 同时将**git describe**的返回放入`buildConfigField`中，作为诸如崩溃日志查询的依据

## 代码

```groovy
/*
 需要git命令行支持
 需要先对git进行全局设置
 需要至少有一次提交
 需要提交中至少有一次tag
 */

static def getGitCommitCount() throws IOException, NumberFormatException {
    'git rev-list --all --count HEAD'.execute().text.toInteger()
}
//有三种情形：
//1. 未添加任何tag               --> 返回空
//2. 刚添加完tag                 --> 返回tag
//3. 距离上一个tag有了至少一次提交 --> 返回完整的结构
//如果编译时未进行提交，则2、3的返回会追加-test，否则则不会
static def getGitDesc() throws IOException {
    'git describe --tags --dirty="-test"'.execute().text.trim()
}

//
// 通过tag生成发布版本, 因此, tag格式为: x.y.z(建议与当前version一致)
//
static def getNewestTag() {
    'git describe --tags'.execute().text.trim()
}

static def getVersionPublish() {
    def versionPublish = "0.0.1"
    try {
        versionPublish = getNewestTag().toString().replace("\"", "")
    } catch (Exception e) {
        e.printStackTrace()
    }
    return versionPublish
}

static def getVersionCode() {
    def versionCode = 1
    try {
        versionCode = getGitCommitCount()
    } catch (Exception e) {
        e.printStackTrace()
    }
    return versionCode
}

static def getVersionDesc() {
    def versionDesc = "null"
    try {
        return getGitDesc().toString().replace("\"", "")
    } catch (Exception e) {
        e.printStackTrace()
    }
    return versionDesc
}

//
// tag格式为x.x.x, 但是作为版本号时只会取前两位, 第三位会被提交数自动替换
//
static def getVersionName() {
    def versionName = "0.0.1"
    try {
        def desc = getGitDesc().toString().replace("\"", "")
        if (!desc.isEmpty()) {
            def isTest = desc.contains("-test")
            desc = desc.replace("-test", "")
            //对应刚打完tag未进行任何提交的情形
            if (!desc.contains('-')) {
                def split = desc.split("\\.")
                if (split.length > 2) {
                    versionName = split[0] + "." + split[1] + ".0"
                } else {
                    versionName = desc + ".0"
                }
            } else {
                //进行局部匹配，返回java.util.regex.Matcher对象
                //^: 开始符
                //.*?: 除换行符外的可能的任意数量的字符，即到下一个字符之间的所有字符
                //-: -
                //\\d*: 可能的多位数字
                def matcher = desc =~ "^.*?-\\d*"
                if (matcher) { //调用的是matcher.find()方法
                    versionName = matcher.group().replace('-', '.')

                    def split = versionName.split("\\.")
                    if (split.length >= 2) {
                        versionName = split[0] + "." + split[1] + "." + split[split.length - 1]
                    }
                }
            }
            if (isTest) {
                versionName += "-test"
            }
        }

    } catch (Exception e) {
        e.printStackTrace()
    }
    return versionName
}

ext {
    gitVersion = [
            versionCode   : getVersionCode(),
            versionName   : getVersionName(),
            versionDesc   : getVersionDesc(),
            versionPublish: getVersionPublish()
    ]
}
```