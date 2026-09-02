#!/usr/bin/env python3
import json
import os
import sqlite3
from http.server import BaseHTTPRequestHandler, HTTPServer

DB_FILE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "suol.db")
HOST = "0.0.0.0"
PORT = 8001


def db():
    conn = sqlite3.connect(DB_FILE)
    conn.row_factory = sqlite3.Row
    return conn


def init_db():
    conn = db()
    try:
        conn.execute(
            """
            CREATE TABLE IF NOT EXISTS items (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                description TEXT,
                created_at TEXT DEFAULT (datetime('now'))
            )
            """
        )
        count = conn.execute("SELECT COUNT(*) FROM items").fetchone()[0]
        if count == 0:
            conn.executemany(
                "INSERT INTO items (name, description) VALUES (?, ?)",
                [
                    ("Первый", "тестовая запись"),
                    ("Второй", "ещё одна запись"),
                ],
            )
        conn.commit()
    finally:
        conn.close()


def insert_item(name, description):
    conn = db()
    try:
        cursor = conn.execute(
            "INSERT INTO items (name, description) VALUES (?, ?)",
            (name, description),
        )
        conn.commit()
        row = conn.execute(
            "SELECT * FROM items WHERE id = ?", (cursor.lastrowid,)
        ).fetchone()
        return dict(row)
    finally:
        conn.close()


def fetch_rows():
    conn = db()
    try:
        rows = conn.execute("SELECT * FROM items").fetchall()
        return [dict(r) for r in rows]
    finally:
        conn.close()


class Handler(BaseHTTPRequestHandler):
    def send_json(self, status, obj):
        payload = json.dumps(obj, ensure_ascii=False).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Content-Length", str(len(payload)))
        self.end_headers()
        self.wfile.write(payload)

    def do_GET(self):
        if self.path.split("?")[0] == "/api/data":
            self.send_json(200, fetch_rows())
        else:
            self.send_error(404)

    def do_POST(self):
        if self.path.split("?")[0] != "/api/data":
            self.send_error(404)
            return
        length = int(self.headers.get("Content-Length", 0) or 0)
        raw = self.rfile.read(length)
        try:
            data = json.loads(raw.decode("utf-8")) if raw else {}
            name = str(data.get("name", "")).strip()
            description = str(data.get("description", "")).strip()
        except (ValueError, UnicodeDecodeError):
            self.send_json(400, {"error": "invalid JSON"})
            return
        if not name:
            self.send_json(400, {"error": "name is required"})
            return
        self.send_json(201, insert_item(name, description))

    def log_message(self, fmt, *args):
        print(f"[{self.log_date_time_string()}] {self.address_string()} {fmt % args}")


if __name__ == "__main__":
    init_db()
    print(f"Сервер запущен на http://{HOST}:{PORT}")
    HTTPServer((HOST, PORT), Handler).serve_forever()