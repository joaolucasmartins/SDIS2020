#!/bin/sh

build_dir="build"
src_dir="src"

mkdir -p "$build_dir"
javac -d "$build_dir" "$src_dir"/*.java "$src_dir"/file/*.java "$src_dir"/message/*.java "$src_dir"/utils/*.java
