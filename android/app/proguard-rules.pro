# R8 混淆规则
# 依赖库（Compose / CameraX / MLKit / OkHttp / DataStore）均自带 consumer rules，
# 正常情况下无需额外配置。若 release 包出现类找不到或反射失效，在这里补充 keep 规则。
