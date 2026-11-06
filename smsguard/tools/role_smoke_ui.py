"""Fail-closed recognition of the ordinary website-update notice in role smoke tests.

Never dismiss Android permissions, role choosers, Play Protect or unknown dialogs.
The immutable 1.1.16 baseline can legitimately advertise public 1.1.17.
"""
PACKAGE = 'com.universalrp.smsguard'
NOTICES = (
    {'புதுப்பிப்பு உள்ளது', 'புதிய பதிப்பு 1.1.17 அதிகாரப்பூர்வ இணையதளத்தில் உள்ளது. உலாவியில் பதிவிறக்குங்கள்; இந்தச் செயலி APK-ஐப் பதிவிறக்கவோ நிறுவவோ செய்யாது.',
     'அதிகாரப்பூர்வ இணையதளத்தைத் திற', 'பிறகு'},
    {'Update available', 'Version 1.1.17 is available on the official website. Download it in your browser; this app does not download or install APKs.',
     'Open official website', 'Later'},
)


def known_update_later(nodes):
    labelled = [n for n in nodes if n.get('text', '').strip()]
    if not labelled or any(n.get('package') != PACKAGE for n in labelled):
        return None
    if {n.get('text') for n in labelled} not in NOTICES:
        return None
    buttons = [n for n in labelled if n.get('text') in ('பிறகு', 'Later')]
    if len(buttons) != 1 or buttons[0].get('enabled') != 'true':
        return None
    return buttons[0]
