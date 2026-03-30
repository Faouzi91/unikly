import os
import re

base = r"c:\Users\aboub\Desktop\unikly\backend\messaging-service\src\main\java\com\unikly\messagingservice"

imports_to_add = {
    "MessageDto": "import com.unikly.messagingservice.adapter.in.web.dto.MessageDto;",
    "ConversationDto": "import com.unikly.messagingservice.adapter.in.web.dto.ConversationDto;",
    "SendMessageRequest": "import com.unikly.messagingservice.adapter.in.web.dto.SendMessageRequest;",
    "GetOrCreateConversationRequest": "import com.unikly.messagingservice.adapter.in.web.dto.GetOrCreateConversationRequest;",
    "MessagingPresenceManager": "import com.unikly.messagingservice.adapter.in.websocket.MessagingPresenceManager;",
    "OutboxEventRepository": "import com.unikly.messagingservice.application.port.out.OutboxEventRepository;",
    "ConversationRepository": "import com.unikly.messagingservice.application.port.out.ConversationRepository;",
    "MessageRepository": "import com.unikly.messagingservice.application.port.out.MessageRepository;"
}

for root, _, files in os.walk(base):
    for f in files:
        if not f.endswith(".java"): continue
        path = os.path.join(root, f)
        
        with open(path, "r", encoding="utf-8") as fn:
            c = fn.read()
            
        modified = False
        lines = c.split("\n")
        
        for class_name, import_stmt in imports_to_add.items():
            if class_name in c and f != class_name + ".java" and import_stmt not in c:
                for i, line in enumerate(lines):
                    if line.startswith("package "):
                        lines.insert(i + 1, import_stmt)
                        modified = True
                        break
        
        # fix OutboxEventRepository extends JpaRepository<OutboxEvent, UUID>
        if f == "OutboxEventRepository.java":
            if "import com.unikly.messagingservice.domain.model.OutboxEvent;" not in c:
                for i, line in enumerate(lines):
                    if line.startswith("package "):
                        lines.insert(i + 1, "import com.unikly.messagingservice.domain.model.OutboxEvent;")
                        modified = True
                        break
        
        if modified:
            with open(path, "w", encoding="utf-8") as fn:
                fn.write("\n".join(lines))

print("Missing imports added")
