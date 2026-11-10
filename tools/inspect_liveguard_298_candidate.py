#!/usr/bin/env python3
"""Live Guard 2.98 only; verify against immutable public 2.97."""
import inspect_battery_voice_candidate as inspector
inspector.APPS['cleaner'] = ('com.universalrp.cleansweep', '2.98', 103, 'live-guard-v2.97', 'CleanSweep-v2.97.apk',
    '121b2c757996910849c76b70f8aa980548e65a84f5fc1e8e0bd6ffda793e92fe', 'CleanSweep-v2.98.apk')
if __name__ == '__main__':
    inspector.main('cleaner')
