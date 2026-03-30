import os
import shutil
import re
import glob

base = r"c:\Users\aboub\Desktop\unikly\backend\payment-service\src\main\java\com\unikly\paymentservice"

def ensure(path):
    os.makedirs(os.path.join(base, path), exist_ok=True)

# 1. Create directories
ensure(r"adapter\in\web\dto")
ensure(r"adapter\out\provider")
ensure(r"application\port\out")
ensure(r"application\service")
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
move_files(r"api", r"adapter\in\web", "*.java") # controllers
move_files(r"application\exception", r"domain\exception", "*.java")
move_files(r"application", r"application\service", "*.java")
move_files(r"domain", r"domain\model", "*.java")
move_files(r"infrastructure", r"application\port\out", "*Repository.java")
move_files(r"infrastructure", r"adapter\out\provider", "StripeClient.java")
move_files(r"infrastructure", r"config", "*Config.java")

# 3. Cleanup old empty dirs
for d in ["api/dto", "api", "application/exception", "infrastructure"]:
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
            
            c = c.replace("com.unikly.paymentservice.api.dto", "com.unikly.paymentservice.adapter.in.web.dto")
            c = c.replace("com.unikly.paymentservice.infrastructure.SecurityConfig", "com.unikly.paymentservice.config.SecurityConfig")
            c = c.replace("com.unikly.paymentservice.infrastructure.KafkaConfig", "com.unikly.paymentservice.config.KafkaConfig")
            c = c.replace("com.unikly.paymentservice.infrastructure.StripeClient", "com.unikly.paymentservice.adapter.out.provider.StripeClient")
            c = c.replace("com.unikly.paymentservice.infrastructure", "com.unikly.paymentservice.application.port.out")
            
            c = c.replace("com.unikly.paymentservice.application.exception", "com.unikly.paymentservice.domain.exception")
            
            c = c.replace("package com.unikly.paymentservice.domain;", "package com.unikly.paymentservice.domain.model;")
            c = c.replace("import com.unikly.paymentservice.domain.", "import com.unikly.paymentservice.domain.model.")
            
            # Application classes -> application.service
            for service in ["PaymentService"]:
                c = c.replace(f"com.unikly.paymentservice.application.{service}", f"com.unikly.paymentservice.application.service.{service}")
            
            c = c.replace("package com.unikly.paymentservice.api;", "package com.unikly.paymentservice.adapter.in.web;")
            c = c.replace("import com.unikly.paymentservice.api.", "import com.unikly.paymentservice.adapter.in.web.")
            
            with open(path, "w") as fn:
                fn.write(c)

replace_in_files()
print("Payment service migrated successfully")
