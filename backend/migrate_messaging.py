import os
import shutil
import glob

base = r"c:\Users\aboub\Desktop\unikly\backend\messaging-service\src\main\java\com\unikly\messagingservice"

def ensure(path):
    os.makedirs(os.path.join(base, path), exist_ok=True)

ensure(r"adapter\in\web\dto")
ensure(r"adapter\in\websocket")
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

move_files(r"api", r"adapter\in\web", "*.java")

move_files(r"application", r"adapter\in\web\dto", "*Dto.java")
move_files(r"application", r"adapter\in\web\dto", "*Request.java")
move_files(r"application", r"application\service", "*Service.java")

move_files(r"domain", r"application\port\out", "*Repository.java")
move_files(r"domain", r"domain\model", "*.java")

move_files(r"infrastructure", r"config", "*Config.java")
move_files(r"infrastructure", r"adapter\in\websocket", "*.java")

for d in ["api", "application", "domain", "infrastructure"]:
    try: os.rmdir(os.path.join(base, os.path.normpath(d)))
    except: pass

def replace_in_files():
    for root, _, files in os.walk(base):
        for f in files:
            if not f.endswith(".java"): continue
            path = os.path.join(root, f)
            with open(path, "r", encoding="utf-8") as fn:
                c = fn.read()
            
            c = c.replace("com.unikly.messagingservice.application.ConversationDto", "com.unikly.messagingservice.adapter.in.web.dto.ConversationDto")
            c = c.replace("com.unikly.messagingservice.application.MessageDto", "com.unikly.messagingservice.adapter.in.web.dto.MessageDto")
            c = c.replace("com.unikly.messagingservice.application.GetOrCreateConversationRequest", "com.unikly.messagingservice.adapter.in.web.dto.GetOrCreateConversationRequest")
            c = c.replace("com.unikly.messagingservice.application.SendMessageRequest", "com.unikly.messagingservice.adapter.in.web.dto.SendMessageRequest")
            
            c = c.replace("com.unikly.messagingservice.infrastructure.SecurityConfig", "com.unikly.messagingservice.config.SecurityConfig")
            c = c.replace("com.unikly.messagingservice.infrastructure.KafkaConfig", "com.unikly.messagingservice.config.KafkaConfig")
            c = c.replace("com.unikly.messagingservice.infrastructure.WebSocketConfig", "com.unikly.messagingservice.config.WebSocketConfig")
            
            c = c.replace("com.unikly.messagingservice.infrastructure.MessagingPresenceManager", "com.unikly.messagingservice.adapter.in.websocket.MessagingPresenceManager")
            c = c.replace("com.unikly.messagingservice.infrastructure.", "com.unikly.messagingservice.adapter.in.websocket.")
            
            c = c.replace("package com.unikly.messagingservice.domain;\n\nimport org.springframework.data.jpa.repository.JpaRepository;", "package com.unikly.messagingservice.application.port.out;\n\nimport org.springframework.data.jpa.repository.JpaRepository;\nimport com.unikly.messagingservice.domain.model.*;")
            
            c = c.replace("com.unikly.messagingservice.domain.ConversationRepository", "com.unikly.messagingservice.application.port.out.ConversationRepository")
            c = c.replace("com.unikly.messagingservice.domain.MessageRepository", "com.unikly.messagingservice.application.port.out.MessageRepository")
            c = c.replace("com.unikly.messagingservice.domain.OutboxEventRepository", "com.unikly.messagingservice.application.port.out.OutboxEventRepository")
            
            c = c.replace("package com.unikly.messagingservice.domain;", "package com.unikly.messagingservice.domain.model;")
            c = c.replace("import com.unikly.messagingservice.domain.", "import com.unikly.messagingservice.domain.model.")
            
            c = c.replace("package com.unikly.messagingservice.application;", "package com.unikly.messagingservice.application.service;")
            c = c.replace("com.unikly.messagingservice.application.ConversationService", "com.unikly.messagingservice.application.service.ConversationService")
            c = c.replace("com.unikly.messagingservice.application.MessageService", "com.unikly.messagingservice.application.service.MessageService")
            
            c = c.replace("package com.unikly.messagingservice.api;", "package com.unikly.messagingservice.adapter.in.web;")
            c = c.replace("import com.unikly.messagingservice.api.", "import com.unikly.messagingservice.adapter.in.web.")
            
            with open(path, "w", encoding="utf-8") as fn:
                fn.write(c)

replace_in_files()

def fix_pkg_names():
    # Fix DTOs that were in application but moved to adapter.in.web.dto
    dto_dir = os.path.join(base, r"adapter\in\web\dto")
    for f in glob.glob(os.path.join(dto_dir, "*.java")):
        with open(f, "r", encoding="utf-8") as fn: c = fn.read()
        c = c.replace("package com.unikly.messagingservice.application.service;", "package com.unikly.messagingservice.adapter.in.web.dto;")
        with open(f, "w", encoding="utf-8") as fn: fn.write(c)

fix_pkg_names()

print("Messaging service migrated successfully")
