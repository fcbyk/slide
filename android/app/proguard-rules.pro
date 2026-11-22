# R8 混淆规则
# 依赖库（Compose / CameraX / MLKit / OkHttp / DataStore）均自带 consumer rules，
# 正常情况下无需额外配置。若 release 包出现类找不到或反射失效，在这里补充 keep 规则。

# MLKit 通过 AndroidManifest 里登记的类名反射实例化 ComponentRegistrar，
# R8 full mode 会删除其无参构造函数，导致 MlKitContext 初始化失败、扫码闪退。
-keep class * implements com.google.firebase.components.ComponentRegistrar {
    public <init>();
}
