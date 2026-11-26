cmake_minimum_required(VERSION 3.22.1)

project(termixpty)

add_library(termixpty SHARED pty.c)

find_library(log-lib log)

target_link_libraries(termixpty ${log-lib})
