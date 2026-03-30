import os
import shutil
import glob

base = r"c:\Users\aboub\Desktop\unikly\backend\search-service\src\main\java\com\unikly\searchservice"

def ensure(path):
    os.makedirs(os.path.join(base, path), exist_ok=True)

ensure(r"adapter\in\web\dto")
ensure(r"adapter\out\messaging")
ensure(r"adapter\out\provider")
ensure(r"application\port\out")
ensure(r"application\service")
ensure(r"domain\model")
ensure(r"config")

def move_files(src_rel, dst_rel, pattern="*.*"):
    src_dir = os.path.join(base, src_rel)
    dst_dir = os.path.join(base, dst_rel)
    if not os.path.exists(src_dir): return
    for p in glob.glob(os.path.join(src_dir, pattern)):
        if os.path.isfile(p):
            shutil.move(p, os.path.join(dst_dir, os.path.basename(p)))

move_files(r"api\dto", r"adapter\in\web\dto", "*.java")
move_files(r"api", r"adapter\in\web", "*.java")

move_files(r"infrastructure", r"config", "*Config.java")
move_files(r"infrastructure", r"adapter\out\messaging", "*Consumer.java")
move_files(r"infrastructure", r"application\port\out", "*Repository.java")
move_files(r"infrastructure", r"adapter\out\provider", "*Client.java")

move_files(r"application", r"application\service", "*.java")

move_files(r"domain", r"domain\model", "*.java")

for d in ["api/dto", "api", "infrastructure", "application", "domain"]:
    try: os.rmdir(os.path.join(base, os.path.normpath(d)))
    except: pass

def replace_in_files():
    for root, _, files in os.walk(base):
        for f in files:
            if not f.endswith(".java"): continue
            path = os.path.join(root, f)
            with open(path, "r", encoding="utf-8") as fn:
                c = fn.read()
            
            # API
            c = c.replace("package com.unikly.searchservice.api.dto;", "package com.unikly.searchservice.adapter.in.web.dto;")
            c = c.replace("package com.unikly.searchservice.api;", "package com.unikly.searchservice.adapter.in.web;")
            c = c.replace("import com.unikly.searchservice.api.dto.", "import com.unikly.searchservice.adapter.in.web.dto.")
            
            # Application
            c = c.replace("package com.unikly.searchservice.application;", "package com.unikly.searchservice.application.service;")
            c = c.replace("import com.unikly.searchservice.application.SearchService;", "import com.unikly.searchservice.application.service.SearchService;")
            
            # Domain
            c = c.replace("package com.unikly.searchservice.domain;", "package com.unikly.searchservice.domain.model;")
            c = c.replace("import com.unikly.searchservice.domain.", "import com.unikly.searchservice.domain.model.")
            
            # Infrastructure
            c = c.replace("package com.unikly.searchservice.infrastructure;", "package com.unikly.searchservice.application.port.out;")
            c = c.replace("import com.unikly.searchservice.infrastructure.", "import com.unikly.searchservice.application.port.out.")
            c = c.replace("com.unikly.searchservice.application.port.out.KafkaConfig", "com.unikly.searchservice.config.KafkaConfig")
            c = c.replace("com.unikly.searchservice.application.port.out.UserProfileClient", "com.unikly.searchservice.adapter.out.provider.UserProfileClient")
            
            with open(path, "w", encoding="utf-8") as fn:
                fn.write(c)

replace_in_files()

def fix_pkg_names():
    msg_dir = os.path.join(base, r"adapter\out\messaging")
    for f in glob.glob(os.path.join(msg_dir, "*.java")):
        with open(f, "r", encoding="utf-8") as fn: c = fn.read()
        c = c.replace("package com.unikly.searchservice.application.port.out;", "package com.unikly.searchservice.adapter.out.messaging;")
        with open(f, "w", encoding="utf-8") as fn: fn.write(c)

    prov_dir = os.path.join(base, r"adapter\out\provider")
    for f in glob.glob(os.path.join(prov_dir, "*.java")):
        with open(f, "r", encoding="utf-8") as fn: c = fn.read()
        c = c.replace("package com.unikly.searchservice.application.port.out;", "package com.unikly.searchservice.adapter.out.provider;")
        with open(f, "w", encoding="utf-8") as fn: fn.write(c)

    cfg_dir = os.path.join(base, r"config")
    for f in glob.glob(os.path.join(cfg_dir, "*.java")):
        with open(f, "r", encoding="utf-8") as fn: c = fn.read()
        c = c.replace("package com.unikly.searchservice.application.port.out;", "package com.unikly.searchservice.config;")
        with open(f, "w", encoding="utf-8") as fn: fn.write(c)

fix_pkg_names()

print("Search service migrated successfully")
