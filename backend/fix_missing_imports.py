import os
import re

base = r"c:\Users\aboub\Desktop\unikly\backend\notification-service\src\main\java\com\unikly\notificationservice"

imports_to_add = {
    "ProcessedEventRepository": "import com.unikly.notificationservice.application.port.out.ProcessedEventRepository;",
    "WebSocketPresenceManager": "import com.unikly.notificationservice.adapter.in.websocket.WebSocketPresenceManager;",
    "JobClientCacheRepository": "import com.unikly.notificationservice.application.port.out.JobClientCacheRepository;",
    "NotificationPreferenceRepository": "import com.unikly.notificationservice.application.port.out.NotificationPreferenceRepository;",
    "NotificationRepository": "import com.unikly.notificationservice.application.port.out.NotificationRepository;",
    "EmailNotificationSender": "import com.unikly.notificationservice.adapter.out.email.EmailNotificationSender;"
}

for root, _, files in os.walk(base):
    for f in files:
        if not f.endswith(".java"): continue
        path = os.path.join(root, f)
        
        with open(path, "r") as fn:
            c = fn.read()
            
        modified = False
        lines = c.split("\n")
        
        for class_name, import_stmt in imports_to_add.items():
            if class_name in c and f != class_name + ".java" and import_stmt not in c:
                # Add import right after the package declaration
                for i, line in enumerate(lines):
                    if line.startswith("package "):
                        lines.insert(i + 1, import_stmt)
                        modified = True
                        break
        
        if modified:
            with open(path, "w") as fn:
                fn.write("\n".join(lines))

print("Missing imports added")
