package com.yevhenii.cluster.planner.server.utils

import scala.annotation.tailrec

trait Trampoline[+A]

case class Continuation[A](next: () => Trampoline[A]) extends Trampoline[A]

case class Finish[A](value: A) extends Trampoline[A]

object Trampoline {
  def calculate[A](t: Trampoline[A]): A = {

    @tailrec def loop(state: Trampoline[A]): A = {
      state match {
        case Continuation(next) =>
          loop(next())
        case Finish(value) =>
          value
      }
    }

    loop(t)
  }
}