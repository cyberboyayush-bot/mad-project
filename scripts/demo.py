#!/usr/bin/env python3
"""
End-to-end UI walkthrough of PocketLedger on a running emulator.

Drives the app through every screen using adb + uiautomator (Compose test tags are exposed
as resource ids), asserts what it sees along the way, and saves screenshots. Pair it with
`scripts/record.sh` to capture a demo video.

Requirements: the API server running on port 8787 with the demo account seeded
(`bun run db:seed` in server/), and an emulator visible to adb.

Usage: python3 scripts/demo.py [--shots DIR] [--pace SECONDS]
"""
import argparse
import os
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

ADB = os.path.join(os.environ.get("ANDROID_HOME", "/opt/android-sdk"), "platform-tools", "adb")
PKG = "me.aysh.pocketledger"
DEMO_EMAIL = "demo@pocketledger.app"
DEMO_PASSWORD = "demo12345"

parser = argparse.ArgumentParser()
parser.add_argument("--shots", default="artifacts/screenshots")
parser.add_argument("--pace", type=float, default=1.2, help="pause between steps, for a watchable video")
args = parser.parse_args()
os.makedirs(args.shots, exist_ok=True)

checks: list[str] = []


def adb(*cmd: str) -> str:
    return subprocess.run([ADB, *cmd], check=True, capture_output=True, text=True).stdout


def shell(*cmd: str) -> str:
    return adb("shell", *cmd)


def pause(factor: float = 1.0) -> None:
    time.sleep(args.pace * factor)


def dump() -> ET.Element:
    for _ in range(5):
        try:
            shell("uiautomator", "dump", "/sdcard/ui.xml")
            xml = shell("cat", "/sdcard/ui.xml")
            return ET.fromstring(xml[xml.index("<?xml"):] if "<?xml" in xml else xml)
        except (subprocess.CalledProcessError, ET.ParseError, ValueError):
            time.sleep(0.5)
    raise RuntimeError("uiautomator dump failed")


def center(node: ET.Element) -> tuple[int, int]:
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.attrib["bounds"]))
    return (x1 + x2) // 2, (y1 + y2) // 2


def find(tag: str | None = None, text: str | None = None, contains: str | None = None, root=None):
    root = root if root is not None else dump()
    for node in root.iter("node"):
        rid = node.attrib.get("resource-id", "")
        label = node.attrib.get("text", "") + " " + node.attrib.get("content-desc", "")
        if tag and (rid == tag or rid.endswith(":id/" + tag)):
            return node
        if text and node.attrib.get("text") == text:
            return node
        if contains and contains.lower() in label.lower():
            return node
    return None


def wait_for(timeout: float = 15, **query) -> ET.Element:
    deadline = time.time() + timeout
    while time.time() < deadline:
        node = find(**query)
        if node is not None:
            return node
        time.sleep(0.4)
    shot("FAILED")
    raise AssertionError(f"Timed out waiting for {query}")


def gone(timeout: float = 10, **query) -> None:
    deadline = time.time() + timeout
    while time.time() < deadline:
        if find(**query) is None:
            return
        time.sleep(0.4)
    raise AssertionError(f"Still visible: {query}")


def tap(tag: str | None = None, factor: float = 1.0, **query) -> None:
    node = wait_for(tag=tag, **query) if tag else wait_for(**query)
    x, y = center(node)
    shell("input", "tap", str(x), str(y))
    pause(factor)


def type_text(value: str) -> None:
    # Type slowly in chunks so it reads naturally on video.
    for chunk in re.findall(r".{1,4}", value):
        shell("input", "text", chunk.replace(" ", "%s").replace("&", "\\&"))
        time.sleep(0.12)
    pause(0.5)


def fill(tag: str, value: str) -> None:
    tap(tag, factor=0.4)
    shell("input", "keyevent", "KEYCODE_MOVE_END")
    for _ in range(40):
        shell("input", "keyevent", "KEYCODE_DEL")
    type_text(value)


def hide_keyboard() -> None:
    if "mInputShown=true" in shell("dumpsys", "input_method"):
        shell("input", "keyevent", "KEYCODE_BACK")
        pause(0.5)


def swipe(x1, y1, x2, y2, ms=350) -> None:
    shell("input", "swipe", str(x1), str(y1), str(x2), str(y2), str(ms))
    pause(0.8)


def scroll_down(amount: int = 900) -> None:
    w, h = screen_size()
    swipe(w // 2, int(h * 0.75), w // 2, int(h * 0.75) - amount, 600)


def scroll_up(amount: int = 900) -> None:
    w, h = screen_size()
    swipe(w // 2, int(h * 0.3), w // 2, int(h * 0.3) + amount, 600)


_size = None


def screen_size() -> tuple[int, int]:
    global _size
    if _size is None:
        w, h = re.search(r"(\d+)x(\d+)", shell("wm", "size")).groups()
        _size = (int(w), int(h))
    return _size


shot_index = 0


def shot(name: str) -> None:
    global shot_index
    shot_index += 1
    path = os.path.join(args.shots, f"{shot_index:02d}-{name}.png")
    with open(path, "wb") as f:
        f.write(subprocess.run([ADB, "exec-out", "screencap", "-p"], check=True, capture_output=True).stdout)


def check(description: str, condition: bool) -> None:
    if not condition:
        shot("FAILED")
        raise AssertionError(description)
    checks.append(description)
    print(f"  ✓ {description}")


def step(title: str) -> None:
    print(f"\n▸ {title}")


# ---------------------------------------------------------------------------

new_email = f"ayush.{int(time.time()) % 100000}@college.edu"

step("Fresh install and launch")
shell("cmd", "uimode", "night", "no")
# Revoking a permission kills the app, so do it before clearing data and launching.
shell("pm", "revoke", PKG, "android.permission.ACCESS_LOCAL_NETWORK")
shell("pm", "clear", PKG)
time.sleep(1)
shell("am", "start", "-W", "-n", f"{PKG}/.MainActivity")
pause(2)
allow = wait_for(tag="com.android.permissioncontroller:id/permission_allow_button")
check("Android 17 local network permission is requested on launch", allow is not None)
shot("permission")
tap("com.android.permissioncontroller:id/permission_allow_button")

step("Welcome")
wait_for(tag="welcome_get_started")
pause(2)
shot("welcome")
check("Welcome screen shows both actions", find(tag="welcome_sign_in") is not None)

step("Sign up — validation")
tap("welcome_get_started")
wait_for(tag="auth_name")
hide_keyboard()
shot("signup")
tap("auth_submit", factor=1.5)
root = dump()
check("Empty sign-up shows field errors", find(contains="Enter your name", root=root) is not None)
shot("signup-errors")
fill("auth_name", "Ayush")
fill("auth_email", "ayush@college")
fill("auth_password", "short")
hide_keyboard()
tap("auth_submit", factor=1.2)
root = dump()
check("Invalid email is rejected", find(contains="valid email", root=root) is not None)
check("Short password is rejected", find(contains="at least 8", root=root) is not None)
shot("signup-invalid")

step("Sign up — duplicate email")
fill("auth_email", DEMO_EMAIL)
fill("auth_password", "Pocket#Ledger26")
hide_keyboard()
tap("auth_submit", factor=2)
check("Duplicate email shows a clear error", wait_for(contains="already exists") is not None)
shot("signup-duplicate")

step("Sign up — success")
fill("auth_email", new_email)
hide_keyboard()
pause(0.5)
shot("signup-filled")
tap("auth_submit", factor=2)
wait_for(tag="home_screen", timeout=20)
pause(1.5)
check("New account lands on Home", True)
shot("home-empty")

step("Add first expense")
tap("add_expense", factor=1.2)
wait_for(tag="editor_amount")
tap("editor_save_expense", factor=1.2)
check("Saving without an amount shows an error", wait_for(contains="Enter an amount") is not None)
tap("editor_amount", factor=0.4)
type_text("249.5")
hide_keyboard()
tap("category_food", factor=0.8)
fill("editor_note", "Lunch at canteen")
hide_keyboard()
shot("editor-new")
tap("editor_save_expense", factor=2)
wait_for(tag="home_screen")
check("Expense appears on Home", wait_for(contains="Lunch at canteen") is not None)
check("Total is formatted in rupees", find(contains="249.50") is not None)
shot("home-first-expense")

step("Add a second expense with another category")
tap("add_expense", factor=1)
wait_for(tag="editor_amount")
type_text("1200")
hide_keyboard()
tap("category_transport", factor=0.8)
fill("editor_note", "Train tickets home")
hide_keyboard()
tap("editor_save_expense", factor=2)
check("Second expense saved", wait_for(contains="Train tickets home") is not None)
shot("home-two-expenses")

step("Set a monthly budget")
tap("tab_profile", factor=1.5)
wait_for(tag="profile_screen")
shot("profile-new-user")
tap("profile_budget", factor=1.2)
wait_for(tag="editor_field")
type_text("8000")
shot("profile-budget-sheet")
tap("editor_save", factor=2)
check("Budget saved", wait_for(contains="8,000") is not None)
tap("tab_home", factor=1.5)
check("Home shows budget remaining", wait_for(contains="left of") is not None)
shot("home-with-budget")

step("Sign out")
tap("tab_profile", factor=1)
tap("profile_sign_out", factor=1.2)
shot("signout-confirm")
tap("confirm_sign_out", factor=2)
wait_for(tag="welcome_get_started")
check("Signing out returns to Welcome", True)

step("Sign in — wrong password")
tap("welcome_sign_in", factor=1)
fill("auth_email", DEMO_EMAIL)
fill("auth_password", "wrong-password")
hide_keyboard()
tap("auth_submit", factor=2)
check("Wrong password shows an error", wait_for(contains="Incorrect email or password") is not None)
shot("signin-error")

step("Sign in — demo account")
fill("auth_password", DEMO_PASSWORD)
hide_keyboard()
shot("signin")
tap("auth_submit", factor=2.5)
wait_for(tag="home_screen", timeout=20)
check("Demo account signed in", wait_for(contains="Ayush") is not None)
pause(1.5)
shot("home")
scroll_down(700)
pause(1)
shot("home-scrolled")
scroll_up(1400)

step("History — search, filter, months")
tap("home_see_all", factor=1.5)
wait_for(tag="history_screen")
shot("history")
tap("history_search", factor=0.5)
type_text("metro")
pause(1.5)
check("Search finds the metro expense", find(contains="Metro") is not None)
shot("history-search")
fill("history_search", "")
hide_keyboard()
tap("chip_food", factor=1.5)
check("Category filter applied", find(contains="Groceries") is not None)
shot("history-filter-food")
tap("chip_food", factor=1.2)
tap("history_prev_month", factor=1.8)
shot("history-previous-month")
tap("history_next_month", factor=1.5)

step("History — swipe to delete and undo")
root = dump()
rows = [n for n in root.iter("node") if n.attrib.get("resource-id", "").startswith("expense_row_")]
check("History lists expenses", len(rows) > 0)
first = rows[0]
x, y = center(first)
w, _ = screen_size()
swipe(int(w * 0.85), y, int(w * 0.1), y, 400)
check("Delete shows an Undo snackbar", wait_for(contains="Undo", timeout=8) is not None)
shot("history-deleted")
tap(contains="Undo", factor=2)
pause(1)
check("Undo restores the expense", find(contains="Chai") is not None)

step("Edit an expense")
root = dump()
row = next(n for n in root.iter("node") if n.attrib.get("resource-id", "").startswith("expense_row_"))
x, y = center(row)
shell("input", "tap", str(x), str(y))
pause(1.5)
wait_for(tag="editor_amount")
shot("editor-edit")
tap("editor_amount", factor=0.3)
shell("input", "keyevent", "KEYCODE_MOVE_END")
for _ in range(10):
    shell("input", "keyevent", "KEYCODE_DEL")
type_text("220")
hide_keyboard()
tap("editor_date", factor=1.5)
shot("editor-date-picker")
shell("input", "keyevent", "KEYCODE_BACK")
pause(1)
tap("editor_save_expense", factor=2)
wait_for(tag="history_screen")
check("Edited amount is shown", wait_for(contains="220") is not None)

step("Stats")
tap("tab_stats", factor=2.5)
wait_for(tag="stats_screen")
shot("stats")
scroll_down(1300)
pause(1.5)
shot("stats-charts")
scroll_up(1800)
tap("stats_prev_month", factor=2)
shot("stats-previous-month")
tap("stats_next_month", factor=1.5)

step("Profile")
tap("tab_profile", factor=1.5)
wait_for(tag="profile_screen")
shot("profile")

step("Dark mode tour")
shell("cmd", "uimode", "night", "yes")
pause(2)
shot("profile-dark")
tap("tab_home", factor=2)
shot("home-dark")
tap("tab_history", factor=2)
shot("history-dark")
tap("tab_stats", factor=2)
scroll_down(900)
pause(1)
shot("stats-dark")
scroll_up(1800)
tap("add_expense", factor=1.5)
wait_for(tag="editor_amount")
type_text("90")
hide_keyboard()
tap("category_health", factor=1)
shot("editor-dark")
tap("editor_close", factor=1.5)
shell("cmd", "uimode", "night", "no")
pause(2)

step("Session persists across restarts")
shell("am", "force-stop", PKG)
shell("am", "start", "-W", "-n", f"{PKG}/.MainActivity")
check("Still signed in after restart", wait_for(tag="home_screen", timeout=20) is not None)
pause(1.5)

step("Back button on Home exits instead of returning to auth")
shell("input", "keyevent", "KEYCODE_BACK")
pause(1.5)
check("App is not showing the sign-in screen after Back", find(tag="auth_email") is None)
shell("am", "start", "-n", f"{PKG}/.MainActivity")
pause(2)

print(f"\nAll {len(checks)} checks passed. Screenshots in {args.shots}")
