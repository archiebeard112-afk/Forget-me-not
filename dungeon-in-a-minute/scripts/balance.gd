extends Node
## Starting numbers, all in one place. Tune these by playtesting.

# Clock
const START_TIME := 60.0          # seconds on the clock at the start of a run
const HIT_TIME_COST := 5.0        # seconds lost per enemy hit
const HIT_INVULN := 0.6           # seconds of invulnerability after a hit
const HIT_KNOCKBACK := 420.0      # push nearby enemies away when you get hit
const LOW_TIME_WARNING := 10.0    # clock turns red below this

# Time shards
const SHARD_TIME := 1.5           # seconds a shard adds to the clock
const SHARD_PICKUP_RADIUS := 30.0
const SHARD_MAGNET_RADIUS := 110.0
const SHARD_MAGNET_SPEED := 650.0
const SHARD_LIFETIME := 8.0       # shards vanish if you don't grab them
const SHARD_BLINK_TIME := 2.0     # shards blink for this long before vanishing

# Player
const PLAYER_SPEED := 270.0
const PLAYER_RADIUS := 16.0

# Auto-weapon: projectile
const FIRE_INTERVAL := 0.4
const FIRE_RANGE := 520.0
const PROJECTILE_SPEED := 760.0
const PROJECTILE_DAMAGE := 1
const PROJECTILE_LIFETIME := 0.9

# Chaser enemy
const CHASER_HP := 2
const CHASER_SPEED := 115.0
const CHASER_SPEED_PER_WAVE := 6.0
const CHASER_RADIUS := 14.0

# Waves
const WAVE_BASE_COUNT := 8        # enemies in wave 1
const WAVE_COUNT_GROWTH := 4      # extra enemies per wave
const WAVE_SPAWN_DURATION := 3.0  # each wave trickles in over this many seconds
const WAVE_GAP := 1.5             # pause between clearing a wave and the next one
const SPAWN_MARGIN := 80.0        # how far past the screen edge enemies appear

# Arena
const ARENA_SIZE := Vector2(1800, 2400)
