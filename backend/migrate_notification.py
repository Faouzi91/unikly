import os
import shutil
import glob

base = r"c:\Users\aboub\Desktop\unikly\backend\notification-service\src\main\java\com\unikly\notificationservice"

def ensure(path):
    os.makedirs(os.path.join(base, path), exist_ok=True)

ensure(r"adapter\in\web\dto")
ensure(r"adapter\in\websocket")
ensure(r"adapter\out\messaging")
ensure(r"adapter\out\email")
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

move_files(r"api\dto", r"adapter\in\web\dto")
move_files(r"api", r"adapter\in\web", "*.java")
move_files(r"application", r"application\service", "*.java")
move_files(r"domain", r"domain\model", "*.java")
move_files(r"infrastructure", r"adapter\in\websocket", "WebSocket*.java")
move_files(r"infrastructure", r"adapter\out\email", "EmailNotificationSender.java")
move_files(r"infrastructure", r"adapter\out\messaging", "*Consumer.java")
move_files(r"infrastructure", r"application\port\out", "*Repository.java")
move_files(r"infrastructure", r"config", "*Config.java")

for d in ["api/dto", "api", "infrastructure"]:
    try: os.rmdir(os.path.join(base, os.path.normpath(d)))
    except: pass

def replace_in_files():
    for root, _, files in os.walk(base):
        for f in files:
            if not f.endswith(".java"): continue
            path = os.path.join(root, f)
            with open(path, "r") as fn:
                c = fn.read()
            
            c = c.replace("com.unikly.notificationservice.api.dto", "com.unikly.notificationservice.adapter.in.web.dto")
            
            c = c.replace("com.unikly.notificationservice.infrastructure.SecurityConfig", "com.unikly.notificationservice.config.SecurityConfig")
            c = c.replace("com.unikly.notificationservice.infrastructure.KafkaConfig", "com.unikly.notificationservice.config.KafkaConfig")
            c = c.replace("com.unikly.notificationservice.infrastructure.WebSocketConfig", "com.unikly.notificationservice.config.WebSocketConfig")
            
            c = c.replace("com.unikly.notificationservice.infrastructure.EmailNotificationSender", "com.unikly.notificationservice.adapter.out.email.EmailNotificationSender")
            
            c = c.replace("package com.unikly.notificationservice.infrastructure;\n\nimport", "package com.unikly.notificationservice.application.port.out;\n\nimport")
            c = c.replace("package com.unikly.notificationservice.infrastructure;", "package com.unikly.notificationservice.application.port.out;")
            
            c = c.replace("com.unikly.notificationservice.infrastructure.", "com.unikly.notificationservice.application.port.out.")
            
            c = c.replace("package com.unikly.notificationservice.domain;", "package com.unikly.notificationservice.domain.model;")
            c = c.replace("import com.unikly.notificationservice.domain.", "import com.unikly.notificationservice.domain.model.")
            
            c = c.replace("package com.unikly.notificationservice.application;", "package com.unikly.notificationservice.application.service;")
            c = c.replace("com.unikly.notificationservice.application.NotificationDeliveryService", "com.unikly.notificationservice.application.service.NotificationDeliveryService")
            
            c = c.replace("package com.unikly.notificationservice.api;", "package com.unikly.notificationservice.adapter.in.web;")
            c = c.replace("import com.unikly.notificationservice.api.", "import com.unikly.notificationservice.adapter.in.web.")
            
            with open(path, "w") as fn:
                fn.write(c)

replace_in_files()

# Fix the WebSocket and Consumer packages separately since they were changed to application.port.out globally
def fix_sub_packages():
    ws_dir = os.path.join(base, r"adapter\in\websocket")
    for f in glob.glob(os.path.join(ws_dir, "*.java")):
        with open(f, "r") as fn: c = fn.read()
        c = c.replace("package com.unikly.notificationservice.application.port.out;", "package com.unikly.notificationservice.adapter.in.websocket;")
        with open(f, "w") as fn: fn.write(c)

    msg_dir = os.path.join(base, r"adapter\out\messaging")
    for f in glob.glob(os.path.join(msg_dir, "*.java")):
        with open(f, "r") as fn: c = fn.read()
        c = c.replace("package com.unikly.notificationservice.application.port.out;", "package com.unikly.notificationservice.adapter.out.messaging;")
        with open(f, "w") as fn: fn.write(c)

fix_sub_packages()
print("Notification service migrated successfully")
