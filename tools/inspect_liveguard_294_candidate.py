#!/usr/bin/env python3
"""Live Guard 2.94 only; verify against immutable public 2.93."""
import inspect_battery_voice_candidate as inspector
inspector.APPS['cleaner'] = ('com.universalrp.cleansweep', '2.94', 99, 'live-guard-v2.93', 'CleanSweep-v2.93.apk',
    '9f8e7936ae98ed5fb43e9087acf6a6de87dc5837fd66c9a97faa0216aa55f012', 'CleanSweep-v2.94.apk')
if __name__ == '__main__':
    inspector.main('cleaner')
