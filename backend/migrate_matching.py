import os
import shutil
import glob

base = r"c:\Users\aboub\Desktop\unikly\backend\matching-service\src\main\java\com\unikly\matchingservice"

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

move_files(r"application", r"adapter\out\provider", "AiMatchingClient.java")
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
            
            c = c.replace("com.unikly.matchingservice.api.dto", "com.unikly.matchingservice.adapter.in.web.dto")
            
            c = c.replace("com.unikly.matchingservice.infrastructure.SecurityConfig", "com.unikly.matchingservice.config.SecurityConfig")
            c = c.replace("com.unikly.matchingservice.infrastructure.KafkaConfig", "com.unikly.matchingservice.config.KafkaConfig")
            
            c = c.replace("package com.unikly.matchingservice.infrastructure;", "package com.unikly.matchingservice.application.port.out;")
            c = c.replace("import com.unikly.matchingservice.infrastructure.", "import com.unikly.matchingservice.application.port.out.")
            
            c = c.replace("package com.unikly.matchingservice.domain;", "package com.unikly.matchingservice.domain.model;")
            c = c.replace("import com.unikly.matchingservice.domain.", "import com.unikly.matchingservice.domain.model.")
            
            c = c.replace("package com.unikly.matchingservice.application;", "package com.unikly.matchingservice.application.service;")
            c = c.replace("import com.unikly.matchingservice.application.", "import com.unikly.matchingservice.application.service.")
            c = c.replace("import com.unikly.matchingservice.application.AiMatchingClient;", "import com.unikly.matchingservice.adapter.out.provider.AiMatchingClient;")
            
            c = c.replace("package com.unikly.matchingservice.api;", "package com.unikly.matchingservice.adapter.in.web;")
            c = c.replace("import com.unikly.matchingservice.api.", "import com.unikly.matchingservice.adapter.in.web.")
            
            with open(path, "w", encoding="utf-8") as fn:
                fn.write(c)

replace_in_files()

def fix_pkg_names():
    msg_dir = os.path.join(base, r"adapter\out\messaging")
    for f in glob.glob(os.path.join(msg_dir, "*.java")):
        with open(f, "r", encoding="utf-8") as fn: c = fn.read()
        c = c.replace("package com.unikly.matchingservice.application.port.out;", "package com.unikly.matchingservice.adapter.out.messaging;")
        with open(f, "w", encoding="utf-8") as fn: fn.write(c)

    prov_dir = os.path.join(base, r"adapter\out\provider")
    for f in glob.glob(os.path.join(prov_dir, "AiMatchingClient.java")):
        with open(f, "r", encoding="utf-8") as fn: c = fn.read()
        c = c.replace("package com.unikly.matchingservice.application.service;", "package com.unikly.matchingservice.adapter.out.provider;")
        with open(f, "w", encoding="utf-8") as fn: fn.write(c)
        
    rep_dir = os.path.join(base, r"application\port\out")
    for f in glob.glob(os.path.join(rep_dir, "*Repository.java")):
        with open(f, "r", encoding="utf-8") as fn: c = fn.read()
        if "import com.unikly.matchingservice.domain.model.*" not in c:
            c = c.replace("import org.springframework.data.jpa.repository.JpaRepository;", "import org.springframework.data.jpa.repository.JpaRepository;\nimport com.unikly.matchingservice.domain.model.*;")
        with open(f, "w", encoding="utf-8") as fn: fn.write(c)

fix_pkg_names()

print("Matching service migrated successfully")
