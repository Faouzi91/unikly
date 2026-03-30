import os
import shutil
import re

BASE_DIR = r"c:\Users\aboub\Desktop\unikly\backend\job-service\src\main\java\com\unikly\jobservice"

# Define where each file should go based on its name and original layer pattern
# Structure: { "filename_pattern": "feature_path_inside_jobservice" }

FEATURE_MAP = {
    # ======= COMMON / CONFIG =======
    "SecurityConfig.java": "common/config",
    "OpenApiConfig.java": "common/config",
    "KafkaConfig.java": "common/config",
    "GlobalExceptionHandler.java": "common/adapter/in/web",
    "PaymentEventConsumer.java": "common/adapter/out/messaging",
    
    # ======= OUTBOX =======
    "ProcessedEventRepository.java": "outbox/adapter/out/persistence",
    "OutboxEventPublisherImpl.java": "outbox/application/service",
    "OutboxEventPublisher.java": "outbox/application/port/out",
    "ProcessedEvent.java": "outbox/domain/model",

    # ======= STORAGE =======
    "StorageController.java": "storage/adapter/in/web",
    "StorageService.java": "storage/application/service",
    "StorageConfig.java": "storage/config",

    # ======= CONTRACT =======
    "ContractController.java": "contract/adapter/in/web",
    "ContractResponse.java": "contract/adapter/in/web/dto",
    "ContractRepository.java": "contract/application/port/out",
    "Contract.java": "contract/domain/model",
    "ContractStatus.java": "contract/domain/model",
    
    # ======= INVITATION =======
    "InvitationController.java": "invitation/adapter/in/web",
    "InvitationResponse.java": "invitation/adapter/in/web/dto",
    "InviteFreelancerRequest.java": "invitation/adapter/in/web/dto",
    "InvitationService.java": "invitation/application/service",
    "InvitationRepository.java": "invitation/application/port/out",
    "Invitation.java": "invitation/domain/model",
    "InvitationStatus.java": "invitation/domain/model",

    # ======= PROPOSAL =======
    "ProposalController.java": "proposal/adapter/in/web",
    "ProposalResponse.java": "proposal/adapter/in/web/dto",
    "SubmitProposalRequest.java": "proposal/adapter/in/web/dto",
    "ProposalStatusUpdateRequest.java": "proposal/adapter/in/web/dto",
    "ProposalMapper.java": "proposal/adapter/in/web/mapper",
    "ProposalService.java": "proposal/application/service",
    "ProposalRepository.java": "proposal/application/port/out",
    "Proposal.java": "proposal/domain/model",
    "ProposalStatus.java": "proposal/domain/model",
    "ProposalImpact.java": "proposal/domain/model",
    "DuplicateProposalException.java": "proposal/domain/exception",
    "InvalidProposalStatusException.java": "proposal/domain/exception",
}

# Assume everything else defaults to "job" if not in the map, but we should define it explicitly ideally.
JOB_FEATURE = "job"

# To avoid false positive replacements, we map exactly the old package path to the new.
# Since we move things into "feature" first, the new package is `com.unikly.jobservice.{feature}.{layer}`

def find_all_java_files(directory):
    files = []
    for root, _, filenames in os.walk(directory):
        for f in filenames:
            if f.endswith(".java") and f != "JobServiceApplication.java":
                files.append(os.path.join(root, f))
    return files

def get_feature_for_file(filepath):
    filename = os.path.basename(filepath)
    if filename in FEATURE_MAP:
        return FEATURE_MAP[filename]
    
    # Heuristics for jobs
    if "Job" in filename or "Transition" in filename or "Edit" in filename:
        # Reconstruct layer path from old location
        rel_path = os.path.relpath(filepath, BASE_DIR)
        layer_path = os.path.dirname(rel_path)
        return os.path.join(JOB_FEATURE, layer_path)
    
    # Special cases
    rel_path = os.path.relpath(filepath, BASE_DIR)
    layer_path = os.path.dirname(rel_path)
    return os.path.join(JOB_FEATURE, layer_path) # Default to job

def main():
    print("Starting refactoring script...")
    java_files = find_all_java_files(BASE_DIR)
    
    print(f"Found {len(java_files)} java files.")

    # 1. Map all files to their new destinations and new package names
    file_mapping = []  # List of tuples: (old_filepath, new_filepath, old_package, new_package, new_import)
    
    for f in java_files:
        filename = os.path.basename(f)
        feature_subpath = get_feature_for_file(f) # e.g., "proposal/domain/model"
        
        # New absolute package path
        new_filepath = os.path.normpath(os.path.join(BASE_DIR, feature_subpath, filename))
        
        # Create directories if they don't exist
        os.makedirs(os.path.dirname(new_filepath), exist_ok=True)
        
        # Old package string (from reading the file)
        with open(f, 'r', encoding='utf-8') as file:
            content = file.read()
        
        pkg_match = re.search(r'package\s+([^;]+);', content)
        if not pkg_match:
            continue
        old_package = pkg_match.group(1).strip()
        
        # Calculate new package string
        # e.g feature_subpath = "proposal\domain\model" -> "com.unikly.jobservice.proposal.domain.model"
        sub_pkg = feature_subpath.replace(os.sep, '.')
        new_package = f"com.unikly.jobservice.{sub_pkg}"
        
        # Full class import path
        old_import = f"{old_package}.{filename[:-5]}"
        new_import = f"{new_package}.{filename[:-5]}"
        
        file_mapping.append({
            "old_filepath": f,
            "new_filepath": new_filepath,
            "filename": filename,
            "old_package": old_package,
            "new_package": new_package,
            "old_import": old_import,
            "new_import": new_import,
            "content": content
        })

    # 2. Update contents (packages and imports)
    print("Updating file contents in memory...")
    for idx, f_map in enumerate(file_mapping):
        content = f_map["content"]
        
        # A) Update package
        content = re.sub(r'package\s+' + re.escape(f_map['old_package']) + r';', f'package {f_map["new_package"]};', content)
        
        # B) Update all other imports
        for other_map in file_mapping:
            if other_map['old_import'] != other_map['new_import']:
                # Negative lookbehind to avoid matching `import x.y.Job` when we have `import x.y.JobService`
                content = re.sub(
                    r'import\s+' + re.escape(other_map['old_import']) + r'(?![\w\.])',
                    f'import {other_map["new_import"]}',
                    content
                )
        
        f_map["content"] = content
        
    print("Moving files and writing new contents...")
    # 3. Write them safely (write then delete old ones if moved)
    # Be careful not to delete a directory before writing!
    
    # To be perfectly safe, write all to temporal or just write the files
    for f_map in file_mapping:
        old_path = f_map["old_filepath"]
        new_path = f_map["new_filepath"]
        
        # If the file hasn't actually 'moved', just overwrite
        if old_path != new_path:
            # write new
            with open(new_path, 'w', encoding='utf-8') as f:
                f.write(f_map["content"])
            # remove old
            try:
                os.remove(old_path)
            except Exception as e:
                pass
        else:
            # write in place
            with open(old_path, 'w', encoding='utf-8') as f:
                f.write(f_map["content"])
                
    print("Script complete!")

if __name__ == "__main__":
    main()
