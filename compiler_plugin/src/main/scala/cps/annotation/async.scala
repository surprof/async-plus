package cps.annotation

import scala.annotation.StaticAnnotation

abstract private class asyncFunction extends StaticAnnotation
class rawasync extends asyncFunction
class async extends asyncFunction
