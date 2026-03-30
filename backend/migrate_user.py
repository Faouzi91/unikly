import os
import shutil
import re
import glob

base = r"c:\Users\aboub\Desktop\unikly\backend\user-service\src\main\java\com\unikly\userservice"

def ensure(path):
    os.makedirs(os.path.join(base, path), exist_ok=True)

# 1. Create directories
ensure(r"adapter\in\web\dto")
ensure(r"adapter\in\web\mapper")
ensure(r"application\service")
ensure(r"application\port\out")
ensure(r"domain\model")
ensure(r"domain\exception")
ensure(r"config")

# Helper to move files
def move_files(src_rel, dst_rel, pattern="*.*"):
    src_dir = os.path.join(base, src_rel)
    dst_dir = os.path.join(base, dst_rel)
    if not os.path.exists(src_dir): return
    for p in glob.glob(os.path.join(src_dir, pattern)):
        if os.path.isfile(p):
            shutil.move(p, os.path.join(dst_dir, os.path.basename(p)))

# 2. Move files
move_files(r"api\dto", r"adapter\in\web\dto")
move_files(r"api\mapper", r"adapter\in\web\mapper")
move_files(r"api", r"adapter\in\web") # controllers + handlers
move_files(r"application", r"application\service", "*.java") # Services
move_files(r"domain", r"domain\model", "*.java") # Entities
move_files(r"infrastructure", r"application\port\out", "*Repository.java")
move_files(r"infrastructure", r"config", "*Config.java")

# 3. Cleanup old empty dirs
for d in ["api/dto", "api/mapper", "api", "infrastructure"]:
    try: os.rmdir(os.path.join(base, os.path.normpath(d)))
    except: pass

# 4. Global string replacement
def replace_in_files():
    for root, _, files in os.walk(base):
        for f in files:
            if not f.endswith(".java"): continue
            path = os.path.join(root, f)
            with open(path, "r") as fn:
                c = fn.read()
            
            c = c.replace("com.unikly.userservice.api.dto", "com.unikly.userservice.adapter.in.web.dto")
            c = c.replace("com.unikly.userservice.api.mapper", "com.unikly.userservice.adapter.in.web.mapper")
            c = c.replace("com.unikly.userservice.infrastructure.SecurityConfig", "com.unikly.userservice.config.SecurityConfig")
            c = c.replace("com.unikly.userservice.infrastructure", "com.unikly.userservice.application.port.out")
            c = c.replace("package com.unikly.userservice.domain;", "package com.unikly.userservice.domain.model;")
            c = c.replace("import com.unikly.userservice.domain.", "import com.unikly.userservice.domain.model.")
            
            # Application classes -> application.service
            for service in ["AuthenticationService", "RegistrationService", "ReviewService", "StorageService", "UserProfileService"]:
                c = c.replace(f"com.unikly.userservice.application.{service}", f"com.unikly.userservice.application.service.{service}")
            
            # Catch raw api package (after subpackages are done)
            c = c.replace("package com.unikly.userservice.api;", "package com.unikly.userservice.adapter.in.web;")
            c = c.replace("import com.unikly.userservice.api.", "import com.unikly.userservice.adapter.in.web.")
            
            with open(path, "w") as fn:
                fn.write(c)

replace_in_files()
print("User service migrated successfully")
