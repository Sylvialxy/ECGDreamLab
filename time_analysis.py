#!/usr/bin/env python3
"""
分析ECG.BIN文件的时间编码
"""

def analyze_ecg_time():
    try:
        with open('ECG.BIN', 'rb') as f:
            data = f.read(20)
        
        print("ECG.BIN文件头部分析:")
        print("完整头部:", ' '.join(f'{b:02X}' for b in data))
        print()
        
        # 字节 1-6: SN码
        sn_bytes = data[0:6]
        print("SN码 (字节1-6):", ' '.join(f'{b:02X}' for b in sn_bytes))
        
        # 字节 7-12: 时间数据
        time_bytes = data[6:12]
        print("时间数据 (字节7-12):", ' '.join(f'{b:02X}' for b in time_bytes))
        print()
        
        # 解析时间数据
        print("时间解析:")
        field_names = ["年份前两位", "年份后两位", "月份", "日期", "小时", "分钟"]
        for i, byte_val in enumerate(time_bytes):
            if i < len(field_names):
                print(f"  字节{i+7}: 0x{byte_val:02X} = {byte_val} ({field_names[i]})")
        
        print()

        # 解析新的时间格式（年份前两位 + 年份后两位 + 月日时分）
        if len(time_bytes) >= 6:
            year_prefix = time_bytes[0]  # 年份前两位
            year_suffix = time_bytes[1]  # 年份后两位
            month = time_bytes[2]
            day = time_bytes[3]
            hour = time_bytes[4]
            minute = time_bytes[5]

            # 构建完整的年份
            full_year = year_prefix * 100 + year_suffix

            print(f"解析的时间数据:")
            print(f"  年份前两位: {year_prefix} (0x{year_prefix:02X})")
            print(f"  年份后两位: {year_suffix} (0x{year_suffix:02X})")
            print(f"  完整年份: {full_year}")
            print(f"  月份: {month}")
            print(f"  日期: {day}")
            print(f"  小时: {hour}")
            print(f"  分钟: {minute}")

            print(f"\n解析的时间: {full_year}年{month}月{day}日 {hour}:{minute:02d}")

            # 按照协议示例格式显示
            expected_format = f"{year_prefix:02X}{year_suffix:02X}{month:02X}{day:02X}{hour:02X}{minute:02X}"
            print(f"协议格式: 0x{expected_format}")

            # 验证时间是否合理
            import datetime
            current_time = datetime.datetime.now()
            print(f"\nCurrent system time: {current_time.strftime('%Y-%m-%d %H:%M')}")

            # 检查时间差异
            try:
                ecg_time = datetime.datetime(full_year, month, day, hour, minute)
                time_diff = abs((current_time - ecg_time).total_seconds())
                print(f"Time difference: {time_diff:.0f} seconds")

                if time_diff < 3600:  # 小于1小时
                    print("✓ Time encoding is correct!")
                else:
                    print("⚠ Large time difference, possible issue")
            except ValueError as e:
                print(f"⚠ Invalid time value: {e}")
        
    except FileNotFoundError:
        print("ECG.BIN文件不存在")
    except Exception as e:
        print(f"分析出错: {e}")

if __name__ == "__main__":
    analyze_ecg_time()
