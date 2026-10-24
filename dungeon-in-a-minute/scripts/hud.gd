extends CanvasLayer
## The clock, wave and kill counters, wave banner and game-over screen.

signal restart_requested

@onready var _time: Label = $Time
@onready var _delta: Label = $Delta
@onready var _wave: Label = $Wave
@onready var _kills: Label = $Kills
@onready var _banner: Label = $Banner
@onready var _game_over: Control = $GameOver
@onready var _summary: Label = $GameOver/Summary

var _can_restart := false


func _ready() -> void:
	_delta.modulate.a = 0.0
	_banner.modulate.a = 0.0
	_game_over.hide()


func update_time(t: float) -> void:
	_time.text = "%.1f" % t
	var low := t < Balance.LOW_TIME_WARNING
	_time.add_theme_color_override("font_color", Color("ff4d6a") if low else Color.WHITE)


## Shows "+1.5" / "-5.0" under the clock.
func time_changed(seconds: float) -> void:
	_delta.text = "%+.1f" % seconds
	_delta.add_theme_color_override("font_color", Color("8affc1") if seconds > 0.0 else Color("ff4d6a"))
	_delta.modulate.a = 1.0
	_delta.create_tween().tween_property(_delta, "modulate:a", 0.0, 0.8).set_delay(0.3)
	if seconds < 0.0:
		_time.pivot_offset = _time.size / 2.0
		_time.scale = Vector2.ONE * 1.3
		_time.create_tween().tween_property(_time, "scale", Vector2.ONE, 0.25)


func set_kills(n: int) -> void:
	_kills.text = "%d kills" % n


func show_wave(n: int) -> void:
	_wave.text = "Wave %d" % n
	_banner.text = "Wave %d" % n
	_banner.modulate.a = 1.0
	_banner.create_tween().tween_property(_banner, "modulate:a", 0.0, 0.6).set_delay(0.8)


func show_game_over(wave: int, kills: int, survived: float) -> void:
	_summary.text = "Time's up\n\nWave %d\n%d kills\nSurvived %.1fs\n\nTap to retry" % [wave, kills, survived]
	_game_over.show()
	_can_restart = false
	get_tree().create_timer(0.8).timeout.connect(func(): _can_restart = true)


func _input(event: InputEvent) -> void:
	if not (_game_over.visible and _can_restart):
		return
	var tapped: bool = event is InputEventScreenTouch and event.pressed
	if tapped or event.is_action_pressed("ui_accept"):
		_can_restart = false
		restart_requested.emit()
