-dontobfuscate
-dontoptimize
-keepattributes *
-keep class app.morphe.** {
  *;
}
-keep class com.google.** {
  *;
}
# QuackJS. It uses no reflection, so this is likely not needed and R8 could shrink it.
# Kept for now until the library is tested with shrinking.
-keep class meow.nyx.** {
  *;
}
# RLX: RLX extension code, see rlx/README.md.
-keep class app.rlx.** {
  *;
}
-dontwarn java.lang.reflect.AnnotatedType
-dontwarn javax.lang.model.element.Modifier
