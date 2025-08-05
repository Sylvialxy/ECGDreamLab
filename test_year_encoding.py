#!/usr/bin/env python3
"""
测试年份编码问题
"""

def test_year_encoding():
    year = 2025
    
    print(f"原始年份: {year}")
    print(f"年份后两位: {year % 100}")
    
    # 模拟Java的toByte()转换
    year_full_byte = year & 0xFF  # 相当于(year).toByte()
    year_mod_byte = (year % 100) & 0xFF  # 相当于(year % 100).toByte()
    
    print(f"完整年份转byte: {year_full_byte} (0x{year_full_byte:02X})")
    print(f"年份后两位转byte: {year_mod_byte} (0x{year_mod_byte:02X})")
    
    # 分析2025转byte的问题
    print(f"\n2025的二进制: {bin(2025)}")
    print(f"2025的十六进制: 0x{2025:X}")
    print(f"2025 & 0xFF = {2025 & 0xFF} (0x{2025 & 0xFF:02X})")
    
    # 分析设备可能的解释
    overflow_value = 2025 & 0xFF
    print(f"\n如果设备将{overflow_value}解释为年份:")
    print(f"  可能显示为: {2000 + overflow_value}年")
    print(f"  或者显示为: {1900 + overflow_value}年")
    
    # 正确的编码
    correct_value = year % 100
    print(f"\n正确的编码 (年份后两位):")
    print(f"  发送值: {correct_value} (0x{correct_value:02X})")
    print(f"  设备应该显示: 20{correct_value}年")

if __name__ == "__main__":
    test_year_encoding()
