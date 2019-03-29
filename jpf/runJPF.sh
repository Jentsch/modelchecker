#!/usr/bin/env bash

~/projects/jpf-core/bin/jpf +classpath=target/scala-2.10/jpf-assembly-0.1.0-SNAPSHOT.jar  test.FuturePhilosophers
~/projects/jpf-core/bin/jpf +classpath=target/scala-2.10/jpf-assembly-0.1.0-SNAPSHOT.jar  test.NativePhilosophers

