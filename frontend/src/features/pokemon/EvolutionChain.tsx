import { ArrowDown, ArrowRight } from 'lucide-react'
import { Link } from 'react-router'

import type { EvolutionEdge, EvolutionNode } from '@/api/types'
import {
  formatEvolutionEdgeLabel,
  formatNationalNumber,
} from '@/lib/format'
import { cn } from '@/lib/utils'

const SPRITE_SIZE = 100

function stageLabel(
  node: EvolutionNode,
  index: number,
  total: number,
): string {
  if (node.isCurrent) {
    return 'Current Pokémon'
  }
  if (index === 0) {
    return 'Base form'
  }
  if (index === total - 1) {
    return 'Final evolution'
  }
  return 'Middle evolution'
}

function EvolutionNodeCard({
  node,
  label,
  condition,
}: {
  node: EvolutionNode
  label: string
  condition?: string
}) {
  return (
    <Link
      to={`/pokemon/${node.nationalNumber}`}
      aria-label={node.name}
      className={cn(
        'flex min-w-0 flex-1 items-center gap-5 rounded-md border p-4 outline-none transition-colors focus-visible:ring-[3px] focus-visible:ring-ring/50 md:gap-6 md:p-5',
        node.isCurrent
          ? 'border-danger-border bg-danger-muted'
          : 'border-border bg-surface',
      )}
    >
      <img
        src={node.imageUrl}
        alt={node.name}
        width={SPRITE_SIZE}
        height={SPRITE_SIZE}
        loading="lazy"
        className="size-[4.75rem] shrink-0 object-contain md:size-[6.25rem]"
        style={{ imageRendering: 'pixelated' }}
      />
      <div className="min-w-0 space-y-2">
        <p className="font-mono text-label tracking-wide text-muted-foreground">
          {formatNationalNumber(node.nationalNumber)}
        </p>
        <p className="text-[1.0625rem] font-semibold text-foreground md:text-title">
          {node.name}
        </p>
        {condition ? (
          <p className="font-mono text-label tracking-wide text-muted-foreground">
            {condition}
          </p>
        ) : null}
        <p
          className={cn(
            'text-caption',
            node.isCurrent ? 'text-primary' : 'text-muted-foreground',
          )}
        >
          {label}
        </p>
      </div>
    </Link>
  )
}

function EdgeLabel({ edge }: { edge: EvolutionEdge }) {
  return (
    <div className="flex h-9 w-full shrink-0 items-center justify-center gap-2 text-muted-foreground md:h-[164px] md:w-[76px]">
      <p className="font-mono text-label tracking-wide whitespace-nowrap">
        {formatEvolutionEdgeLabel(edge)}
      </p>
      <ArrowDown className="size-3.5 md:hidden" aria-hidden />
      <ArrowRight className="hidden size-3.5 md:block" aria-hidden />
    </div>
  )
}

function SectionHeading({ stagesLabel }: { stagesLabel: string }) {
  return (
    <div className="flex items-center justify-between gap-3">
      <h2 className="text-title font-semibold text-foreground md:text-2xl">
        Evolution chain
      </h2>
      <p className="font-mono text-label tracking-wide text-muted-foreground uppercase">
        {stagesLabel}
      </p>
    </div>
  )
}

function LinearSequence({
  nodes,
  edges,
}: {
  nodes: EvolutionNode[]
  edges: EvolutionEdge[]
}) {
  return (
    <div className="flex flex-col md:flex-row md:items-start">
      {nodes.map((node, index) => (
        <div
          key={node.nationalNumber}
          className="flex flex-col md:min-w-0 md:flex-1 md:flex-row md:items-start"
        >
          <EvolutionNodeCard
            node={node}
            label={stageLabel(node, index, nodes.length)}
          />
          {edges[index] ? <EdgeLabel edge={edges[index]!} /> : null}
        </div>
      ))}
    </div>
  )
}

function BranchChildren({ edges }: { edges: EvolutionEdge[] }) {
  return (
    <div className="space-y-4">
      <h3 className="text-body font-semibold text-foreground">Evolves into</h3>
      <ul className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
        {edges.map((edge) => (
          <li key={edge.node.nationalNumber} className="flex">
            <EvolutionNodeCard
              node={edge.node}
              label={edge.node.isCurrent ? 'Current Pokémon' : 'Evolution'}
              condition={formatEvolutionEdgeLabel(edge)}
            />
          </li>
        ))}
      </ul>
    </div>
  )
}

/**
 * Linear path (0–1 child per node) → sequence with Lv./trigger connectors.
 * When a node has more than one child → parent card + “Evolves into” grid
 * (condition inside each child card; no sibling arrows).
 */
export function EvolutionChain({ root }: { root: EvolutionNode }) {
  const prefixNodes: EvolutionNode[] = []
  const prefixEdges: EvolutionEdge[] = []
  let current = root

  while (current.children.length === 1) {
    prefixNodes.push(current)
    prefixEdges.push(current.children[0]!)
    current = current.children[0]!.node
  }

  if (current.children.length === 0) {
    const nodes = [...prefixNodes, current]
    return (
      <section className="space-y-5">
        <SectionHeading stagesLabel={`${nodes.length} stages`} />
        <LinearSequence nodes={nodes} edges={prefixEdges} />
      </section>
    )
  }

  // Branch: current has 2+ children.
  const branchParentNodes = [...prefixNodes, current]
  const formCount = branchParentNodes.length + current.children.length

  return (
    <section className="space-y-5">
      <SectionHeading stagesLabel={`${formCount} forms`} />
      {prefixNodes.length > 0 ? (
        <LinearSequence
          nodes={branchParentNodes}
          edges={prefixEdges}
        />
      ) : (
        <EvolutionNodeCard
          node={current}
          label={current.isCurrent ? 'Current Pokémon' : 'Base form'}
        />
      )}
      <BranchChildren edges={current.children} />
    </section>
  )
}
