extends Node2D
## Rectangular arena centred on the origin: a floor grid and four walls.

const WALL := 64.0
const GRID := 120.0

var rect := Rect2(-Balance.ARENA_SIZE / 2.0, Balance.ARENA_SIZE)


func _ready() -> void:
	var body := StaticBody2D.new()
	body.collision_layer = 1
	body.collision_mask = 0
	add_child(body)
	var size := rect.size
	for wall in [
		Rect2(rect.position.x - WALL, rect.position.y - WALL, size.x + WALL * 2, WALL),
		Rect2(rect.position.x - WALL, rect.end.y, size.x + WALL * 2, WALL),
		Rect2(rect.position.x - WALL, rect.position.y, WALL, size.y),
		Rect2(rect.end.x, rect.position.y, WALL, size.y),
	]:
		var shape := RectangleShape2D.new()
		shape.size = wall.size
		var col := CollisionShape2D.new()
		col.shape = shape
		col.position = wall.get_center()
		body.add_child(col)


func _draw() -> void:
	draw_rect(rect, Color("1a1a2a"))
	var line := Color(1, 1, 1, 0.04)
	var x := rect.position.x + GRID
	while x < rect.end.x:
		draw_line(Vector2(x, rect.position.y), Vector2(x, rect.end.y), line, 2.0)
		x += GRID
	var y := rect.position.y + GRID
	while y < rect.end.y:
		draw_line(Vector2(rect.position.x, y), Vector2(rect.end.x, y), line, 2.0)
		y += GRID
	draw_rect(rect, Color("3a3a5a"), false, 6.0)
