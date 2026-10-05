import argparse

from platform_support_common import collect_changes, FileModification, Changes


def apply_changes_locally(changes: Changes):
    """Apply changes to local filesystem."""
    for path, (modification, content) in changes.items():
        if modification == FileModification.Delete:
            if path.exists():
                print(f"Deleting: {path}")
                path.unlink()
                # Remove empty parent directories
                try:
                    if path.parent.exists() and not any(path.parent.iterdir()):
                        path.parent.rmdir()
                except OSError:
                    pass
        elif modification == FileModification.Change:
            print(f"Writing: {path}")
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(content)


def parse_args():
    parser = argparse.ArgumentParser(description="Drop platform support locally (no external services)")
    parser.add_argument("--platform_version", type=int, required=True, help="Major version of IntelliJ platform")
    return parser.parse_args()


def main():
    args = parse_args()
    platform_version = args.platform_version

    print(f"Collecting changes for platform {platform_version}...")
    changes = collect_changes(platform_version)

    if not changes:
        print("No changes to apply")
        return

    print(f"Found {len(changes)} files to modify")

    # Apply changes to filesystem
    apply_changes_locally(changes)
    print("\nChanges applied but not committed. Review and commit manually.")


if __name__ == '__main__':
    main()
