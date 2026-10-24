extends Node2D
## Run controller: the clock, waves, kills and game over.

## Fired when every enemy in a wave is dead. Build step 5 (level-up cards)
## hooks in here.
signal wave_cleared(wave: int)

const ChaserScene := preload("res://scenes/chaser.tscn")
const ShardScene := preload("res://scenes/time_shard.tscn")

enum State { GAP, SPAWNING, FIGHTING, OVER }

var time_left := Balance.START_TIME
var wave := 0
var kills := 0

var _state := State.GAP
var _gap_timer := 0.5
var _to_spawn := 0
var _spawn_interval := 0.0
var _spawn_timer := 0.0
var _survived := 0.0

@onready var _arena := $Arena
@onready var _player := $Player
@onready var _camera: Camera2D = $Player/Camera
@onready var _enemies := $Enemies
@onready var _pickups := $Pickups
@onready var _hud := $HUD
@onready var _joystick := $Controls/Joystick


func _ready() -> void:
	_player.joystick = _joystick
	_player.projectile_parent = $Projectiles
	_player.hit.connect(_on_player_hit)
	_hud.restart_requested.connect(_on_restart_requested)

	var r: Rect2 = _arena.rect
	_camera.limit_left = int(r.position.x)
	_camera.limit_top = int(r.position.y)
	_camera.limit_right = int(r.end.x)
	_camera.limit_bottom = int(r.end.y)

	_hud.update_time(time_left)
	_hud.set_kills(kills)


func _process(delta: float) -> void:
	if _state == State.OVER:
		return
	_survived += delta
	_set_time(time_left - delta)
	if _state == State.OVER:
		return

	match _state:
		State.GAP:
			_gap_timer -= delta
			if _gap_timer <= 0.0:
				_start_wave()
		State.SPAWNING:
			_spawn_timer -= delta
			while _spawn_timer <= 0.0 and _to_spawn > 0:
				_spawn_chaser()
				_to_spawn -= 1
				_spawn_timer += _spawn_interval
			if _to_spawn == 0:
				_state = State.FIGHTING
		State.FIGHTING:
			if get_tree().get_nodes_in_group("enemies").is_empty():
				_state = State.GAP
				_gap_timer = Balance.WAVE_GAP
				wave_cleared.emit(wave)


func _start_wave() -> void:
	wave += 1
	_to_spawn = Balance.WAVE_BASE_COUNT + (wave - 1) * Balance.WAVE_COUNT_GROWTH
	_spawn_interval = Balance.WAVE_SPAWN_DURATION / _to_spawn
	_spawn_timer = 0.0
	_state = State.SPAWNING
	_hud.show_wave(wave)


func _spawn_chaser() -> void:
	var enemy := ChaserScene.instantiate()
	enemy.global_position = _spawn_position()
	enemy.target = _player
	enemy.speed = Balance.CHASER_SPEED + (wave - 1) * Balance.CHASER_SPEED_PER_WAVE
	enemy.died.connect(_on_enemy_died)
	_enemies.add_child(enemy)


## A random point just off screen, inside the arena.
func _spawn_position() -> Vector2:
	var view := get_viewport_rect().size / _camera.zoom
	var dist := view.length() / 2.0 + Balance.SPAWN_MARGIN
	var inner: Rect2 = _arena.rect.grow(-Balance.CHASER_RADIUS * 2.0)
	var p := Vector2.ZERO
	for i in 8:
		p = _player.global_position + Vector2.from_angle(randf() * TAU) * dist
		if inner.has_point(p):
			return p
	return p.clamp(inner.position, inner.end)


func _set_time(t: float) -> void:
	time_left = maxf(t, 0.0)
	_hud.update_time(time_left)
	if time_left <= 0.0:
		_game_over()


func _on_player_hit() -> void:
	_hud.time_changed(-Balance.HIT_TIME_COST)
	_set_time(time_left - Balance.HIT_TIME_COST)


func _on_enemy_died(at: Vector2) -> void:
	kills += 1
	_hud.set_kills(kills)
	var shard := ShardScene.instantiate()
	shard.position = at
	shard.player = _player
	shard.collected.connect(_on_shard_collected)
	_pickups.call_deferred("add_child", shard)


func _on_shard_collected(seconds: float) -> void:
	if _state == State.OVER:
		return
	_hud.time_changed(seconds)
	_set_time(time_left + seconds)


func _game_over() -> void:
	if _state == State.OVER:
		return
	_state = State.OVER
	_joystick.release()
	get_tree().paused = true
	_hud.show_game_over(wave, kills, _survived)


func _on_restart_requested() -> void:
	get_tree().paused = false
	get_tree().reload_current_scene()
