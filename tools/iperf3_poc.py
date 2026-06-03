"""
iperf3 PoC - TCP/UDP 测试 + 输出解析验证
模拟 Android ProcessBuilder 的执行方式
"""
import subprocess
import sys
import os
import time
import re
import json
import signal

IPERF3_PATH = os.path.join(os.path.dirname(__file__), "iperf3-windows", "iperf-3.1.3-win64", "iperf3.exe")
DEFAULT_SERVER = "iperf.he.net"
DEFAULT_PORT = 5201
OUTPUT_DIR = os.path.join(os.path.dirname(__file__), "test-outputs")

os.makedirs(OUTPUT_DIR, exist_ok=True)

# SpeedInterval 解析正则
TCP_INTERVAL_RE = re.compile(
    r'\[\s*(\d+)\]\s+(\d+\.\d+)-(\d+\.\d+)\s+sec\s+([\d.]+)\s+(\w+)\s+([\d.]+)\s+(\w+/sec)'
)
UDP_INTERVAL_RE = re.compile(
    r'\[\s*(\d+)\]\s+(\d+\.\d+)-(\d+\.\d+)\s+sec\s+([\d.]+)\s+(\w+)\s+([\d.]+)\s+(\w+/sec)\s+([\d.]+)\s+ms\s+(\d+)/(\d+)\s+\(([\d.]+)%\)'
)

def to_bits_per_second(value, unit):
    """将带宽值转换为 bits/sec"""
    unit = unit.lower().replace("/sec", "")
    multipliers = {
        "bits": 1,
        "kbits": 1000,
        "mbits": 1_000_000,
        "gbits": 1_000_000_000,
        "bytes": 8,
        "kbytes": 8000,
        "mbytes": 8_000_000,
        "gbytes": 8_000_000_000,
    }
    return value * multipliers.get(unit, 1)

def parse_interval(line):
    """解析 interval 输出行"""
    # UDP 格式（有 jitter 和 loss）
    m = UDP_INTERVAL_RE.match(line)
    if m:
        return {
            "type": "udp",
            "stream_id": int(m.group(1)),
            "second_index": int(float(m.group(2))),
            "duration": float(m.group(3)) - float(m.group(2)),
            "transfer_bytes": float(m.group(4)),
            "transfer_unit": m.group(5),
            "bits_per_second": to_bits_per_second(float(m.group(6)), m.group(7)),
            "bitrate_value": float(m.group(6)),
            "bitrate_unit": m.group(7),
            "jitter_ms": float(m.group(8)),
            "lost": int(m.group(9)),
            "total": int(m.group(10)),
            "packet_loss_percent": float(m.group(11)),
        }
    # TCP 格式
    m = TCP_INTERVAL_RE.match(line)
    if m:
        return {
            "type": "tcp",
            "stream_id": int(m.group(1)),
            "second_index": int(float(m.group(2))),
            "duration": float(m.group(3)) - float(m.group(2)),
            "transfer_bytes": float(m.group(4)),
            "transfer_unit": m.group(5),
            "bits_per_second": to_bits_per_second(float(m.group(6)), m.group(7)),
            "bitrate_value": float(m.group(6)),
            "bitrate_unit": m.group(7),
        }
    return None

def run_test(args, timeout=30, test_name="test"):
    """运行 iperf3 测试，返回 (stdout, stderr, exit_code, raw_output)"""
    cmd = [IPERF3_PATH] + args
    print(f"\n{'='*60}")
    print(f"TEST: {test_name}")
    print(f"CMD:  {' '.join(cmd)}")
    print(f"{'='*60}")
    
    raw_lines = []
    intervals = []
    
    try:
        proc = subprocess.Popen(
            cmd,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            bufsize=1,
        )
        
        start_time = time.time()
        for line in proc.stdout:
            line = line.rstrip()
            raw_lines.append(line)
            parsed = parse_interval(line)
            if parsed:
                intervals.append(parsed)
                if parsed["type"] == "tcp":
                    mbps = parsed["bits_per_second"] / 1_000_000
                    print(f"  [{parsed['second_index']}s] {mbps:.1f} Mbps")
                elif parsed["type"] == "udp":
                    mbps = parsed["bits_per_second"] / 1_000_000
                    print(f"  [{parsed['second_index']}s] {mbps:.1f} Mbps | jitter={parsed['jitter_ms']:.3f}ms loss={parsed['packet_loss_percent']:.1f}%")
            
            if time.time() - start_time > timeout:
                print(f"  TIMEOUT after {timeout}s, killing...")
                proc.kill()
                break
        
        proc.wait(timeout=5)
        exit_code = proc.returncode
    except subprocess.TimeoutExpired:
        proc.kill()
        exit_code = -1
    except Exception as e:
        print(f"  ERROR: {e}")
        exit_code = -1
    
    raw_output = "\n".join(raw_lines)
    
    # 保存 raw output
    output_file = os.path.join(OUTPUT_DIR, f"{test_name}_{int(time.time())}.txt")
    with open(output_file, "w", encoding="utf-8") as f:
        f.write(raw_output)
    
    print(f"\n  Exit code: {exit_code}")
    print(f"  Intervals parsed: {len(intervals)}")
    print(f"  Raw output saved: {output_file}")
    
    return raw_output, exit_code, intervals

def test_tcp_forward(server=DEFAULT_SERVER, port=DEFAULT_PORT, duration=5):
    """P1: TCP 正向测速"""
    return run_test(
        ["-c", server, "-p", str(port), "-t", str(duration), "-i", "1"],
        test_name="P1_tcp_forward"
    )

def test_tcp_reverse(server=DEFAULT_SERVER, port=DEFAULT_PORT, duration=5):
    """P2: TCP 反向测速"""
    return run_test(
        ["-c", server, "-p", str(port), "-t", str(duration), "-i", "1", "-R"],
        test_name="P2_tcp_reverse"
    )

def test_udp_forward(server=DEFAULT_SERVER, port=DEFAULT_PORT, duration=5):
    """P3: UDP 正向测速"""
    return run_test(
        ["-c", server, "-p", str(port), "-t", str(duration), "-i", "1", "--udp", "-b", "10M"],
        test_name="P3_udp_forward"
    )

def test_udp_reverse(server=DEFAULT_SERVER, port=DEFAULT_PORT, duration=5):
    """P4: UDP 反向测速"""
    return run_test(
        ["-c", server, "-p", str(port), "-t", str(duration), "-i", "1", "--udp", "-b", "10M", "-R"],
        test_name="P4_udp_reverse"
    )

def test_json_output(server=DEFAULT_SERVER, port=DEFAULT_PORT, duration=3):
    """A5: JSON 输出解析"""
    raw, exit_code, _ = run_test(
        ["-c", server, "-p", str(port), "-t", str(duration), "-J"],
        test_name="A5_json_output"
    )
    if exit_code == 0:
        try:
            data = json.loads(raw)
            print(f"\n  JSON keys: {list(data.keys())}")
            if "intervals" in data:
                print(f"  Intervals count: {len(data['intervals'])}")
            if "end" in data:
                print(f"  End keys: {list(data['end'].keys())}")
        except json.JSONDecodeError as e:
            print(f"\n  JSON parse error: {e}")
    return raw, exit_code

def test_cancel(duration=60, cancel_after=3):
    """C1: 运行中取消"""
    print(f"\n{'='*60}")
    print(f"TEST: C1_cancel (start {duration}s test, cancel after {cancel_after}s)")
    print(f"{'='*60}")
    
    cmd = [IPERF3_PATH, "-c", DEFAULT_SERVER, "-p", str(DEFAULT_PORT), "-t", str(duration), "-i", "1"]
    proc = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
    
    time.sleep(cancel_after)
    print(f"  Sending terminate after {cancel_after}s...")
    proc.terminate()
    
    try:
        proc.wait(timeout=5)
        print(f"  Process exited with code: {proc.returncode}")
    except subprocess.TimeoutExpired:
        print(f"  Process didn't exit in 5s, force killing...")
        proc.kill()
        proc.wait(timeout=3)
        print(f"  Process killed, exit code: {proc.returncode}")
    
    return proc.returncode

def test_timeout(timeout=5):
    """C2: 超时处理"""
    print(f"\n{'='*60}")
    print(f"TEST: C2_timeout (60s test with {timeout}s timeout)")
    print(f"{'='*60}")
    
    cmd = [IPERF3_PATH, "-c", DEFAULT_SERVER, "-p", str(DEFAULT_PORT), "-t", "60", "-i", "1"]
    proc = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
    
    start = time.time()
    while time.time() - start < timeout:
        if proc.poll() is not None:
            print(f"  Process ended naturally at {time.time()-start:.1f}s")
            return proc.returncode
        time.sleep(0.5)
    
    print(f"  Timeout reached, killing...")
    proc.kill()
    proc.wait(timeout=3)
    elapsed = time.time() - start
    print(f"  Process killed at {elapsed:.1f}s, exit code: {proc.returncode}")
    return proc.returncode

def test_sctp(server=DEFAULT_SERVER, port=DEFAULT_PORT):
    """P7: SCTP 能力检测"""
    raw, exit_code, _ = run_test(
        ["-c", server, "-p", str(port), "-t", "3", "--sctp"],
        timeout=10,
        test_name="P7_sctp"
    )
    if exit_code != 0:
        print(f"\n  SCTP not supported (expected on Windows)")
        for line in raw.split("\n"):
            if "error" in line.lower() or "protocol" in line.lower():
                print(f"  Error line: {line.strip()}")
    return raw, exit_code

if __name__ == "__main__":
    server = sys.argv[1] if len(sys.argv) > 1 else DEFAULT_SERVER
    port = int(sys.argv[2]) if len(sys.argv) > 2 else DEFAULT_PORT
    
    print(f"iperf3 PoC Test Suite")
    print(f"Server: {server}:{port}")
    print(f"iperf3: {IPERF3_PATH}")
    
    results = {}
    
    # P1: TCP 正向
    try:
        raw, code, intervals = test_tcp_forward(server, port, duration=5)
        results["P1_tcp_forward"] = {"exit_code": code, "intervals": len(intervals)}
    except Exception as e:
        results["P1_tcp_forward"] = {"error": str(e)}
    
    # P2: TCP 反向
    try:
        raw, code, intervals = test_tcp_reverse(server, port, duration=5)
        results["P2_tcp_reverse"] = {"exit_code": code, "intervals": len(intervals)}
    except Exception as e:
        results["P2_tcp_reverse"] = {"error": str(e)}
    
    # P3: UDP 正向
    try:
        raw, code, intervals = test_udp_forward(server, port, duration=5)
        results["P3_udp_forward"] = {"exit_code": code, "intervals": len(intervals)}
    except Exception as e:
        results["P3_udp_forward"] = {"error": str(e)}
    
    # P4: UDP 反向
    try:
        raw, code, intervals = test_udp_reverse(server, port, duration=5)
        results["P4_udp_reverse"] = {"exit_code": code, "intervals": len(intervals)}
    except Exception as e:
        results["P4_udp_reverse"] = {"error": str(e)}
    
    # A5: JSON 输出
    try:
        raw, code = test_json_output(server, port, duration=3)
        results["A5_json"] = {"exit_code": code}
    except Exception as e:
        results["A5_json"] = {"error": str(e)}
    
    # C1: 取消测试
    try:
        code = test_cancel(duration=60, cancel_after=3)
        results["C1_cancel"] = {"exit_code": code}
    except Exception as e:
        results["C1_cancel"] = {"error": str(e)}
    
    # C2: 超时测试
    try:
        code = test_timeout(timeout=5)
        results["C2_timeout"] = {"exit_code": code}
    except Exception as e:
        results["C2_timeout"] = {"error": str(e)}
    
    # P7: SCTP
    try:
        raw, code = test_sctp(server, port)
        results["P7_sctp"] = {"exit_code": code}
    except Exception as e:
        results["P7_sctp"] = {"error": str(e)}
    
    # 汇总
    print(f"\n{'='*60}")
    print(f"SUMMARY")
    print(f"{'='*60}")
    for name, result in results.items():
        status = "PASS" if result.get("exit_code") == 0 or "error" not in result else "CHECK"
        if "error" in result:
            status = "ERROR"
        print(f"  {name}: {status} {result}")
