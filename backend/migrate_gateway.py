import os
import shutil
import glob

base = r"c:\Users\aboub\Desktop\unikly\backend\gateway\src\main\java\com\unikly\gateway"

def ensure(path):
    os.makedirs(os.path.join(base, path), exist_ok=True)

ensure(r"adapter\in\web\filter")

def move_files(src_rel, dst_rel, pattern="*.*"):
    src_dir = os.path.join(base, src_rel)
    dst_dir = os.path.join(base, dst_rel)
    if not os.path.exists(src_dir): return
    for p in glob.glob(os.path.join(src_dir, pattern)):
        if os.path.isfile(p):
            shutil.move(p, os.path.join(dst_dir, os.path.basename(p)))

move_files(r"controller", r"adapter\in\web", "*.java")
move_files(r"filter", r"adapter\in\web\filter", "*.java")

for d in ["controller", "filter"]:
    try: os.rmdir(os.path.join(base, os.path.normpath(d)))
    except: pass

def replace_in_files():
    for root, _, files in os.walk(base):
        for f in files:
            if not f.endswith(".java"): continue
            path = os.path.join(root, f)
            with open(path, "r", encoding="utf-8") as fn:
                c = fn.read()
            
            c = c.replace("package com.unikly.gateway.controller;", "package com.unikly.gateway.adapter.in.web;")
            c = c.replace("package com.unikly.gateway.filter;", "package com.unikly.gateway.adapter.in.web.filter;")
            
            with open(path, "w", encoding="utf-8") as fn:
                fn.write(c)

replace_in_files()

print("Gateway migrated successfully")
