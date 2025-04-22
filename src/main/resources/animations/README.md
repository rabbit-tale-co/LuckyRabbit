# Lucky Rabbit Animations

This directory contains license files for animations used in the Lucky Rabbit plugin.

## Default Animation

- `horizontal.yml` - The default horizontal spinning animation (available to all users)

## Premium Animations

These premium animations are only available with valid license files from our Patreon:

- `circle.yml` - Circular spinning animation with items moving in a ring
- `three_in_row.yml` - Triple slot-machine style animation with jackpot mechanics
- `pin_point.yml` - Single item animation with rapid cycling
- `cascade.yml` - Items cascade across the screen in sequence

## How Animation Licenses Work

Each animation is protected by a validation system:

1. Every animation license file contains a unique validation key and checksum
2. These must match the values embedded in the plugin code
3. The validation keys change with each major plugin update
4. Only official license files from our Patreon will work

## Using Premium Animations

1. Download the animation license file from our Patreon
2. Place it in this directory (plugins/LuckyRabbit/animations/)
3. Restart the server or use `/luckyrabbit reload`
4. If the license is valid, the animation will be available

## License File Structure

```yaml
# Animation license file
name: "Animation Name"
id: "ANIMATION_ID"
description: "Description of the animation"
enabled: true

# Validation data - must match the code in the plugin
validation_key: "XX-0000-XXXX-0000-XXXX"  # Unique key for this animation
checksum: "0123456789abcdef0123456789abcdef"  # Integrity check
api_version: "1.0.0"  # Must match the plugin's API version

# Metadata
author: "RabbitTale"
website: "https://patreon.com/rabbittale"
```

## Support

For questions or issues with animation licenses, please contact support on our Discord server or visit our Patreon page to get access to premium animation licenses.
